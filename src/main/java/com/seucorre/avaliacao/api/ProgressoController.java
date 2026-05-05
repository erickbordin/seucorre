package com.seucorre.avaliacao.api;

import com.seucorre.avaliacao.application.ProgressoAppService;
import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
import com.seucorre.usuario.domain.Usuario;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/progresso")
@RequiredArgsConstructor
public class ProgressoController {

    private final ProgressoAppService service;

    @GetMapping("/historico")
    public ResponseEntity<List<ProgressoSemanalDTO>> listarHistorico(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(service.listarHistoricoProgresso(usuarioLogado.getId()));
    }

    @GetMapping("/planos/{planoId}/semanas/{semana}")
    public ResponseEntity<ProgressoSemanalDTO> buscarSemana(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable UUID planoId,
            @PathVariable @Min(1) Integer semana
    ) {
        return ResponseEntity.ok(service.buscarProgressoSemana(usuarioLogado.getId(), planoId, semana));
    }

    @GetMapping("/visao-geral")
    public ResponseEntity<VisaoGeralProgressoDTO> buscarVisaoGeral(@AuthenticationPrincipal Usuario usuarioLogado) {
        List<ProgressoSemanalDTO> historico = service.listarHistoricoProgresso(usuarioLogado.getId());
        return ResponseEntity.ok(VisaoGeralProgressoDTO.from(historico));
    }

    record VisaoGeralProgressoDTO(
            int semanasRegistradas,
            BigDecimal volumeTotalKm,
            BigDecimal melhorPaceMedio,
            double taxaAdesaoMedia,
            int totalTreinos,
            ProgressoSemanalDTO semanaMaisRecente,
            List<ProgressoSemanalDTO> historico
    ) {

        static VisaoGeralProgressoDTO from(List<ProgressoSemanalDTO> historico) {
            BigDecimal volumeTotalKm = historico.stream()
                    .map(ProgressoSemanalDTO::volumeKm)
                    .filter(valor -> valor != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal melhorPaceMedio = historico.stream()
                    .map(ProgressoSemanalDTO::paceMedio)
                    .filter(valor -> valor != null)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            double taxaAdesaoMedia = historico.stream()
                    .mapToDouble(ProgressoSemanalDTO::taxaAdesao)
                    .average()
                    .orElse(0d);

            int totalTreinos = historico.stream()
                    .mapToInt(ProgressoSemanalDTO::totalTreinos)
                    .sum();

            ProgressoSemanalDTO semanaMaisRecente = historico.stream()
                    .max(Comparator
                            .comparing(ProgressoSemanalDTO::numeroSemana, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(ProgressoSemanalDTO::dataInicioSemana, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);

            return new VisaoGeralProgressoDTO(
                    historico.size(),
                    volumeTotalKm.signum() == 0 ? BigDecimal.ZERO : volumeTotalKm.setScale(2, RoundingMode.HALF_UP),
                    melhorPaceMedio,
                    BigDecimal.valueOf(taxaAdesaoMedia).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                    totalTreinos,
                    semanaMaisRecente,
                    historico
            );
        }
    }
}
