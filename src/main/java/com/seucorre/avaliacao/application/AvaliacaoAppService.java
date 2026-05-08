package com.seucorre.avaliacao.application;

import com.seucorre.infra.cache.CacheConfig;
import com.seucorre.infra.events.EventPublisher;
import com.seucorre.infra.notification.NotificacaoService;
import com.seucorre.avaliacao.application.dto.AnaliseRiscoDTO;
import com.seucorre.avaliacao.application.dto.CheckinSemanalRequest;
import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.event.PlanoReescritoEvent;
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
    private final EventPublisher eventPublisher;
    private final RegistroRepository registroRepository;
    private final NotificacaoService notificacaoService;

    @Transactional
    public AnaliseRiscoDTO processarCheckin(CheckinSemanalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de check-in é obrigatória.");
        }

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        PlanoTreino planoTreino = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new EntityNotFoundException("Plano de treino não encontrado."));

        if (planoTreino.getUsuario() == null || !usuario.getId().equals(planoTreino.getUsuario().getId())) {
            throw new BusinessRuleException("O plano informado não pertence ao usuário.");
        }

        CheckinSemanal checkinSemanal = checkinRepository.findByPlanoIdAndSemana(request.planoId(), request.semana())
                .orElseGet(CheckinSemanal::new);

        preencherCheckin(checkinSemanal, request, usuario, planoTreino);

        ProgressoSemanal progressoSemanal = progressoAppService.buscarProgressoSemana(planoTreino.getId(), request.semana());
        checkinSemanal.setAnaliseIA(gerarAnaliseIA(checkinSemanal, progressoSemanal));

        if (checkinSemanal.precisaReescreverPlano()) {
            PlanoTreino planoReescrito = geradorPlanoIA.reescreverPlano(planoTreino, checkinSemanal);
            persistirReescrita(planoTreino, planoReescrito, checkinSemanal);
        } else {
            checkinSemanal.setPlanoReescrito(false);
        }

        CheckinSemanal checkinSalvo = checkinRepository.save(checkinSemanal);
        return AnaliseRiscoDTO.from(checkinSalvo);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PLANO_ATIVO_CACHE, key = "#usuarioId")
    public AnaliseRiscoDTO processarCheckin(UUID usuarioId, CheckinSemanalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de check-in é obrigatória.");
        }
        if (!usuarioId.equals(request.usuarioId())) {
            throw new BusinessRuleException("O usuário informado no corpo da requisição difere do usuário autenticado.");
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

    private String gerarAnaliseIA(CheckinSemanal checkinSemanal, ProgressoSemanal progressoSemanal) {
        String analiseCheckin = geradorPlanoIA.gerarAnaliseCheckin(checkinSemanal);
        if (progressoSemanal == null) {
            return analiseCheckin;
        }

        String alertaSemanal = progressoAppService.gerarAlertaSemanal(
                checkinSemanal.getPlano() == null ? null : checkinSemanal.getPlano().getId(),
                checkinSemanal.getSemana()
        );

        String sugestaoProgresso = geradorPlanoIA.sugerirAjuste(progressoSemanal);
        String analiseComSugestao = analiseCheckin;
        if (sugestaoProgresso != null && !sugestaoProgresso.isBlank()) {
            if (analiseComSugestao == null || analiseComSugestao.isBlank()) {
                analiseComSugestao = sugestaoProgresso;
            } else {
                analiseComSugestao = analiseComSugestao + "\nAjuste semanal: " + sugestaoProgresso;
            }
        }
        if (alertaSemanal == null || alertaSemanal.isBlank()) {
            return analiseComSugestao;
        }
        if (analiseComSugestao == null || analiseComSugestao.isBlank()) {
            return alertaSemanal;
        }
        return analiseComSugestao + "\n" + alertaSemanal;
    }

    private void persistirReescrita(PlanoTreino planoOriginal,
                                    PlanoTreino planoReescrito,
                                    CheckinSemanal checkinSemanal) {
        if (planoReescrito == null) {
            throw new BusinessRuleException("A IA não retornou um plano reescrito válido.");
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
