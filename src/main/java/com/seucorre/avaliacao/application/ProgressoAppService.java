package com.seucorre.avaliacao.application;

import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.avaliacao.infrastructure.ProgressoRepository;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.PlanoRepository;
import com.seucorre.treino.infrastructure.RegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressoAppService {

    private final ProgressoRepository progressoRepository;
    private final RegistroRepository registroRepository;
    private final PlanoRepository planoRepository;

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

        if (numeroSemana > 1) {
            progressoRepository.findByPlanoIdAndNumeroSemana(planoTreino.getId(), numeroSemana - 1)
                    .ifPresent(progressoAnterior -> progressoSemanal.ultrapassouRegra10Porcento(progressoAnterior));
        }

        return progressoRepository.save(progressoSemanal);
    }

    @Transactional(readOnly = true)
    public ProgressoSemanal buscarProgressoSemana(UUID planoId, Integer semana) {
        if (planoId == null || semana == null) {
            return null;
        }
        return progressoRepository.findByPlanoIdAndNumeroSemana(planoId, semana).orElse(null);
    }

    @Transactional(readOnly = true)
    public ProgressoSemanalDTO buscarProgressoSemana(UUID usuarioId, UUID planoId, Integer semana) {
        PlanoTreino planoTreino = planoRepository.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException("Plano de treino não encontrado."));

        if (planoTreino.getUsuario() == null || !usuarioId.equals(planoTreino.getUsuario().getId())) {
            throw new BusinessRuleException("O plano informado não pertence ao usuário.");
        }

        ProgressoSemanal progressoSemanal = progressoRepository.findByPlanoIdAndNumeroSemana(planoId, semana)
                .orElseThrow(() -> new EntityNotFoundException("Progresso semanal não encontrado."));

        return ProgressoSemanalDTO.from(progressoSemanal);
    }

    @Transactional(readOnly = true)
    public List<ProgressoSemanalDTO> listarHistoricoProgresso(UUID usuarioId) {
        return progressoRepository.findByPlanoUsuarioIdOrderByNumeroSemanaAsc(usuarioId).stream()
                .map(ProgressoSemanalDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressoSemanal> listarUltimosProgressosDoPlano(UUID planoId) {
        if (planoId == null) {
            return List.of();
        }

        return progressoRepository.findTop3ByPlanoIdOrderByNumeroSemanaDesc(planoId).stream()
                .sorted(Comparator.comparing(ProgressoSemanal::getNumeroSemana, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public String gerarAlertaSemanal(UUID planoId, Integer numeroSemana) {
        ProgressoSemanal atual = buscarProgressoSemana(planoId, numeroSemana);
        if (atual == null || numeroSemana == null || numeroSemana <= 1) {
            return null;
        }

        ProgressoSemanal anterior = buscarProgressoSemana(planoId, numeroSemana - 1);
        if (anterior == null) {
            return null;
        }

        if (atual.ultrapassouRegra10Porcento(anterior)) {
            return String.format(
                    "Alerta: a semana %d ultrapassou a Regra dos 10%% com variacao de %.2f%% no volume.",
                    numeroSemana,
                    atual.calcularVariacaoVolume(anterior)
            );
        }
        return null;
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
