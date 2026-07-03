package com.seucorre.avaliacao.application;

import com.seucorre.infra.cache.CacheConfig;
import com.seucorre.infra.events.EventPublisher;
import com.seucorre.infra.ia.IAProperties;
import com.seucorre.infra.notification.NotificacaoService;
import com.seucorre.avaliacao.application.dto.AnaliseRiscoDTO;
import com.seucorre.avaliacao.application.dto.CheckinSemanalRequest;
import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.event.PlanoReescritoEvent;
import com.seucorre.shared.domain.event.CheckinProcessadoEvent;
import com.seucorre.avaliacao.infrastructure.CheckinRepository;
import com.seucorre.shared.domain.enums.NivelRisco;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.domain.GeradorPlanoIA;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.PlanoRepository;
import com.seucorre.treino.infrastructure.RegistroRepository;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvaliacaoAppService {

    private final ProgressoAppService progressoAppService;
    private final CheckinRepository checkinRepository;
    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeradorPlanoIA geradorPlanoIA;
    private final ManualCheckinAnalysisService manualCheckinAnalysisService;
    private final IAProperties iaProperties;
    private final EventPublisher eventPublisher;
    private final RegistroRepository registroRepository;
    private final NotificacaoService notificacaoService;

    @Transactional
    public AnaliseRiscoDTO processarCheckin(CheckinSemanalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de check-in e obrigatoria.");
        }

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
        PlanoTreino planoTreino = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new EntityNotFoundException("Plano de treino nao encontrado."));

        if (planoTreino.getUsuario() == null || !usuario.getId().equals(planoTreino.getUsuario().getId())) {
            throw new BusinessRuleException("O plano informado nao pertence ao usuario.");
        }

        CheckinSemanal checkinSemanal = checkinRepository.findByPlanoIdAndSemana(request.planoId(), request.semana())
                .orElseGet(CheckinSemanal::new);

        preencherCheckin(checkinSemanal, request, usuario, planoTreino);

        ProgressoSemanal progressoSemanal = progressoAppService.buscarProgressoSemana(planoTreino.getId(), request.semana());
        String alertaSemanal = progressoSemanal == null
                ? null
                : progressoAppService.gerarAlertaSemanal(planoTreino.getId(), request.semana());
        checkinSemanal.setAnaliseIA(gerarAnalise(checkinSemanal, progressoSemanal, alertaSemanal));

        if (checkinSemanal.precisaReescreverPlano()) {
            if (iaProperties.usaModoManual()) {
                checkinSemanal.setPlanoReescrito(false);
            } else {
                List<ProgressoSemanal> progressosRecentes = progressoAppService.listarUltimosProgressosDoPlano(planoTreino.getId());
                PlanoTreino planoReescrito = geradorPlanoIA.reescreverPlano(planoTreino, checkinSemanal, progressosRecentes);
                persistirReescrita(planoTreino, planoReescrito, checkinSemanal);
            }
        } else {
            checkinSemanal.setPlanoReescrito(false);
        }

        CheckinSemanal checkinSalvo = checkinRepository.save(checkinSemanal);
        publicarCheckinProcessado(checkinSalvo);
        return AnaliseRiscoDTO.from(checkinSalvo);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PLANO_ATIVO_CACHE, key = "#usuarioId")
    public AnaliseRiscoDTO processarCheckin(UUID usuarioId, CheckinSemanalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de check-in e obrigatoria.");
        }
        if (!usuarioId.equals(request.usuarioId())) {
            throw new BusinessRuleException("O usuario informado no corpo da requisicao difere do usuario autenticado.");
        }
        return processarCheckin(request);
    }

    @Transactional(readOnly = true)
    public List<AnaliseRiscoDTO> listarHistoricoCheckins(UUID usuarioId) {
        return checkinRepository.findByUsuarioIdOrderBySemanaAsc(usuarioId).stream()
                .map(AnaliseRiscoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressoSemanalDTO> listarHistoricoProgresso(UUID usuarioId) {
        return progressoAppService.listarHistoricoProgresso(usuarioId);
    }

    @Transactional(readOnly = true)
    public int enviarLembretesMatinais() {
        LocalDate hoje = LocalDate.now();
        int lembretesEnviados = 0;

        for (PlanoTreino planoTreino : listarPlanosAtivos()) {
            Integer semanaAtual = calcularSemanaAtual(planoTreino, hoje);
            if (semanaAtual == null) {
                continue;
            }

            List<SessaoTreino> treinosHoje = planoTreino.obterSessoesDaSemana(semanaAtual).stream()
                    .filter(sessaoTreino -> hoje.equals(sessaoTreino.getDataPrevista()))
                    .filter(SessaoTreino::estaPendente)
                    .toList();

            if (treinosHoje.isEmpty() || planoTreino.getUsuario() == null || planoTreino.getUsuario().getId() == null) {
                continue;
            }

            notificacaoService.enviarLembreteTreino(planoTreino.getUsuario().getId(), semanaAtual, treinosHoje.size());
            lembretesEnviados++;
        }

        return lembretesEnviados;
    }

    @Transactional
    public int abrirCheckinsSemanais() {
        LocalDate hoje = LocalDate.now();
        int checkinsAbertos = 0;

        for (PlanoTreino planoTreino : listarPlanosAtivos()) {
            Integer semanaAtual = calcularSemanaAtual(planoTreino, hoje);
            if (semanaAtual == null || planoTreino.getUsuario() == null || planoTreino.getUsuario().getId() == null) {
                continue;
            }

            if (checkinRepository.findByPlanoIdAndSemana(planoTreino.getId(), semanaAtual).isPresent()) {
                continue;
            }

            CheckinSemanal checkinSemanal = new CheckinSemanal();
            checkinSemanal.setUsuario(planoTreino.getUsuario());
            checkinSemanal.setPlano(planoTreino);
            checkinSemanal.setSemana(semanaAtual);
            checkinSemanal.setDataCheckin(hoje);
            checkinSemanal.setAvaliacao("Check-in semanal aberto automaticamente.");
            checkinSemanal.setPlanoReescrito(false);
            checkinRepository.save(checkinSemanal);

            notificacaoService.enviarAberturaCheckin(planoTreino.getUsuario().getId(), semanaAtual);
            checkinsAbertos++;
        }

        return checkinsAbertos;
    }

    @Transactional
    public int recalcularProgressosAutomaticos() {
        LocalDate hoje = LocalDate.now();
        int progressosRecalculados = 0;

        for (PlanoTreino planoTreino : listarPlanosAtivos()) {
            Integer semanaAtual = calcularSemanaAtual(planoTreino, hoje);
            if (semanaAtual == null) {
                continue;
            }

            List<RegistroTreino> registrosSemana = registroRepository.findByPlanoIdAndNumeroSemana(planoTreino.getId(), semanaAtual);
            if (registrosSemana.isEmpty()) {
                continue;
            }

            progressoAppService.atualizarProgressoSemanal(registrosSemana.get(registrosSemana.size() - 1));
            progressosRecalculados++;
        }

        return progressosRecalculados;
    }

    private void preencherCheckin(CheckinSemanal checkinSemanal,
                                  CheckinSemanalRequest request,
                                  Usuario usuario,
                                  PlanoTreino planoTreino) {
        checkinSemanal.setUsuario(usuario);
        checkinSemanal.setPlano(planoTreino);
        checkinSemanal.setSemana(request.semana());
        checkinSemanal.setDataCheckin(request.dataCheckin());
        checkinSemanal.setNivelEsforco(request.nivelEsforco());
        checkinSemanal.setNivelDor(request.nivelDor());
        checkinSemanal.setHorasSonoSemana(request.horasSonoSemana());
        checkinSemanal.setAvaliacao(request.avaliacao());
    }

    private String gerarAnalise(CheckinSemanal checkinSemanal,
                                ProgressoSemanal progressoSemanal,
                                String alertaSemanal) {
        if (iaProperties.usaModoManual()) {
            return manualCheckinAnalysisService.gerarAnalise(checkinSemanal, progressoSemanal, alertaSemanal);
        }

        String analiseCheckin = geradorPlanoIA.gerarAnaliseCheckin(checkinSemanal);
        if (progressoSemanal == null) {
            return combinarAnaliseComAlerta(analiseCheckin, alertaSemanal);
        }

        String sugestaoProgresso = geradorPlanoIA.sugerirAjuste(progressoSemanal);
        String analiseComSugestao = combinarAnaliseComSugestao(analiseCheckin, sugestaoProgresso);
        return combinarAnaliseComAlerta(analiseComSugestao, alertaSemanal);
    }

    private String combinarAnaliseComSugestao(String analiseBase, String sugestaoProgresso) {
        if (sugestaoProgresso == null || sugestaoProgresso.isBlank()) {
            return analiseBase;
        }
        if (analiseBase == null || analiseBase.isBlank()) {
            return sugestaoProgresso;
        }
        return analiseBase + "\nAjuste semanal: " + sugestaoProgresso;
    }

    private String combinarAnaliseComAlerta(String analiseBase, String alertaSemanal) {
        if (alertaSemanal == null || alertaSemanal.isBlank()) {
            return analiseBase;
        }
        if (analiseBase == null || analiseBase.isBlank()) {
            return alertaSemanal;
        }
        return analiseBase + "\n" + alertaSemanal;
    }

    private void persistirReescrita(PlanoTreino planoOriginal,
                                    PlanoTreino planoReescrito,
                                    CheckinSemanal checkinSemanal) {
        if (planoReescrito == null) {
            throw new BusinessRuleException("A IA nao retornou um plano reescrito valido.");
        }

        checkinSemanal.setPlanoReescrito(true);
        planoOriginal.setStatus(StatusPlano.CANCELADO);
        planoRepository.save(planoOriginal);

        planoReescrito.setStatus(StatusPlano.ATIVO);
        PlanoTreino novoPlanoSalvo = planoRepository.save(planoReescrito);

        NivelRisco nivelRisco = checkinSemanal.avaliarNivelRisco();
        eventPublisher.publish(new PlanoReescritoEvent(
                checkinSemanal.getUsuario() == null ? null : checkinSemanal.getUsuario().getId(),
                planoOriginal.getId(),
                novoPlanoSalvo.getId(),
                checkinSemanal.getId(),
                checkinSemanal.getSemana(),
                nivelRisco
        ));
    }

    private void publicarCheckinProcessado(CheckinSemanal checkinSemanal) {
        eventPublisher.publish(new CheckinProcessadoEvent(
                checkinSemanal.getUsuario() == null ? null : checkinSemanal.getUsuario().getId(),
                checkinSemanal.getPlano() == null ? null : checkinSemanal.getPlano().getId(),
                checkinSemanal.getId(),
                checkinSemanal.getSemana(),
                checkinSemanal.avaliarNivelRisco(),
                checkinSemanal.getPlanoReescrito()
        ));
    }

    private List<PlanoTreino> listarPlanosAtivos() {
        return planoRepository.findByStatus(StatusPlano.ATIVO).stream()
                .filter(Objects::nonNull)
                .filter(PlanoTreino::estaNoPrazo)
                .filter(planoTreino -> planoTreino.getUsuario() != null)
                .toList();
    }

    private Integer calcularSemanaAtual(PlanoTreino planoTreino, LocalDate referencia) {
        if (planoTreino == null || referencia == null || planoTreino.getDataInicio() == null) {
            return null;
        }
        if (referencia.isBefore(planoTreino.getDataInicio())) {
            return null;
        }

        long semanasDesdeInicio = ChronoUnit.WEEKS.between(planoTreino.getDataInicio(), referencia);
        int semanaAtual = Math.toIntExact(semanasDesdeInicio) + 1;

        if (planoTreino.getTotalSemanas() != null && semanaAtual > planoTreino.getTotalSemanas()) {
            return null;
        }
        return semanaAtual;
    }
}
