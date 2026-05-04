package com.seucorre.avaliacao.application;

import com.seucorre.avaliacao.application.dto.AnaliseRiscoDTO;
import com.seucorre.avaliacao.application.dto.CheckinSemanalRequest;
import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
import com.seucorre.avaliacao.application.event.PlanoReescritoEvent;
import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.avaliacao.infrastructure.CheckinRepository;
import com.seucorre.avaliacao.infrastructure.ProgressoRepository;
import com.seucorre.shared.domain.enums.NivelRisco;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.shared.domain.enums.StatusTreino;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvaliacaoAppService {

    private final ProgressoRepository progressoRepository;
    private final RegistroRepository registroRepository;
    private final CheckinRepository checkinRepository;
    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeradorPlanoIA geradorPlanoIA;
    private final ApplicationEventPublisher eventPublisher;

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

        ProgressoSemanal progressoSemanal = buscarProgressoSemana(planoTreino.getId(), request.semana());
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

    @Transactional(readOnly = true)
    public List<AnaliseRiscoDTO> listarHistoricoCheckins(UUID usuarioId) {
        return checkinRepository.findByUsuarioIdOrderBySemanaAsc(usuarioId).stream()
                .map(AnaliseRiscoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressoSemanalDTO> listarHistoricoProgresso(UUID usuarioId) {
        return progressoRepository.findByPlanoUsuarioIdOrderByNumeroSemanaAsc(usuarioId).stream()
                .map(ProgressoSemanalDTO::from)
                .toList();
    }

    @Transactional
    public ProgressoSemanal atualizarProgressoSemanal(RegistroTreino registroTreino) {
        if (registroTreino == null) {
            throw new IllegalArgumentException("Registro de treino é obrigatório.");
        }

        SessaoTreino sessaoTreino = registroTreino.getSessaoTreino();
        if (sessaoTreino == null || sessaoTreino.getPlano() == null || sessaoTreino.getNumeroSemana() == null) {
            throw new IllegalArgumentException("Registro precisa estar vinculado a uma sessão válida.");
        }

        PlanoTreino planoTreino = sessaoTreino.getPlano();
        Integer numeroSemana = sessaoTreino.getNumeroSemana();

        ProgressoSemanal progressoSemanal = progressoRepository.findByPlanoIdAndNumeroSemana(planoTreino.getId(), numeroSemana)
                .orElseGet(ProgressoSemanal::new);
        progressoSemanal.setPlano(planoTreino);
        progressoSemanal.setNumeroSemana(numeroSemana);
        progressoSemanal.setDataInicioSemana(calcularInicioSemana(planoTreino, numeroSemana, sessaoTreino));

        List<RegistroTreino> registrosSemana = registroRepository.findByPlanoIdAndNumeroSemana(planoTreino.getId(), numeroSemana);
        progressoSemanal.setVolumeKm(calcularVolume(registrosSemana));
        progressoSemanal.setPaceMedio(calcularPaceMedio(registrosSemana));
        progressoSemanal.setFcRepousoMedio(planoTreino.getUsuario() == null ? null : planoTreino.getUsuario().getFcRepouso());
        progressoSemanal.setTreinosConcluidos(contarStatus(registrosSemana, StatusTreino.CONCLUIDO));
        progressoSemanal.setTreinosParciais(contarStatus(registrosSemana, StatusTreino.PARCIAL));
        progressoSemanal.setTreinosPerdidos(contarStatus(registrosSemana, StatusTreino.PERDIDO));

        // O valor é persistido como histórico real; a checagem da regra serve para alimentar alertas posteriores.
        if (numeroSemana > 1) {
            progressoRepository.findByPlanoIdAndNumeroSemana(planoTreino.getId(), numeroSemana - 1)
                    .ifPresent(progressoAnterior -> progressoSemanal.ultrapassouRegra10Porcento(progressoAnterior));
        }

        return progressoRepository.save(progressoSemanal);
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

    private ProgressoSemanal buscarProgressoSemana(UUID planoId, Integer semana) {
        if (planoId == null || semana == null) {
            return null;
        }
        return progressoRepository.findByPlanoIdAndNumeroSemana(planoId, semana).orElse(null);
    }

    private String gerarAnaliseIA(CheckinSemanal checkinSemanal, ProgressoSemanal progressoSemanal) {
        String analiseCheckin = geradorPlanoIA.gerarAnaliseCheckin(checkinSemanal);
        if (progressoSemanal == null) {
            return analiseCheckin;
        }

        String sugestaoProgresso = geradorPlanoIA.sugerirAjuste(progressoSemanal);
        if (sugestaoProgresso == null || sugestaoProgresso.isBlank()) {
            return analiseCheckin;
        }
        if (analiseCheckin == null || analiseCheckin.isBlank()) {
            return sugestaoProgresso;
        }
        return analiseCheckin + "\nAjuste semanal: " + sugestaoProgresso;
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
        eventPublisher.publishEvent(new PlanoReescritoEvent(
                checkinSemanal.getUsuario() == null ? null : checkinSemanal.getUsuario().getId(),
                planoOriginal.getId(),
                novoPlanoSalvo.getId(),
                checkinSemanal.getId(),
                checkinSemanal.getSemana(),
                nivelRisco
        ));
    }

    private LocalDate calcularInicioSemana(PlanoTreino planoTreino, Integer numeroSemana, SessaoTreino sessaoTreino) {
        return planoTreino.obterSessoesDaSemana(numeroSemana).stream()
                .map(SessaoTreino::getDataPrevista)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(sessaoTreino.getDataPrevista());
    }

    private BigDecimal calcularVolume(List<RegistroTreino> registrosSemana) {
        BigDecimal volume = registrosSemana.stream()
                .map(RegistroTreino::getDistanciaRealKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return volume.signum() == 0 ? null : volume.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPaceMedio(List<RegistroTreino> registrosSemana) {
        List<Integer> paces = registrosSemana.stream()
                .map(RegistroTreino::getPaceMedioReal)
                .filter(Objects::nonNull)
                .toList();
        if (paces.isEmpty()) {
            return null;
        }

        BigDecimal soma = paces.stream()
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(paces.size()), 2, RoundingMode.HALF_UP);
    }

    private int contarStatus(List<RegistroTreino> registrosSemana, StatusTreino statusTreino) {
        return (int) registrosSemana.stream()
                .map(RegistroTreino::getStatus)
                .filter(statusTreino::equals)
                .count();
    }
}
