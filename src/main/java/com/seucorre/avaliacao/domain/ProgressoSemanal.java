package com.seucorre.avaliacao.domain;

import com.seucorre.treino.domain.PlanoTreino;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "progresso_semanal")
@Data
public class ProgressoSemanal {

    private static final BigDecimal LIMITE_REGRA_DEZ_POR_CENTO = BigDecimal.valueOf(1.10d);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PlanoTreino plano;

    @Column(name = "numero_semana")
    private Integer numeroSemana;

    @Column(name = "data_inicio_semana")
    private LocalDate dataInicioSemana;

    @Column(name = "volume_km", precision = 6, scale = 2)
    private BigDecimal volumeKm;

    @Column(name = "pace_medio", precision = 5, scale = 2)
    private BigDecimal paceMedio;

    @Column(name = "fc_repouso_medio")
    private Integer fcRepousoMedio;

    @Column(name = "treinos_concluidos")
    private Integer treinosConcluidos;

    @Column(name = "treinos_parciais")
    private Integer treinosParciais;

    @Column(name = "treinos_perdidos")
    private Integer treinosPerdidos;

    public int totalTreinos() {
        return inteiroOuZero(treinosConcluidos)
                + inteiroOuZero(treinosParciais)
                + inteiroOuZero(treinosPerdidos);
    }

    public double calcularTaxaAdesao() {
        int totalTreinos = totalTreinos();
        if (totalTreinos == 0) {
            return 0d;
        }

        int treinosAderidos = inteiroOuZero(treinosConcluidos) + inteiroOuZero(treinosParciais);
        return BigDecimal.valueOf(treinosAderidos)
                .multiply(CEM)
                .divide(BigDecimal.valueOf(totalTreinos), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public double calcularVariacaoVolume(ProgressoSemanal anterior) {
        BigDecimal volumeAnterior = volumeValido(anterior);
        BigDecimal volumeAtual = volumeAtualOuZero();
        if (volumeAnterior == null || volumeAnterior.signum() <= 0 || volumeAtual.signum() <= 0) {
            return 0d;
        }

        return volumeAtual.subtract(volumeAnterior)
                .multiply(CEM)
                .divide(volumeAnterior, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public boolean ultrapassouRegra10Porcento(ProgressoSemanal anterior) {
        BigDecimal volumeAnterior = volumeValido(anterior);
        BigDecimal volumeAtual = volumeAtualOuZero();
        if (volumeAnterior == null || volumeAnterior.signum() <= 0 || volumeAtual.signum() <= 0) {
            return false;
        }

        BigDecimal limiteAceito = volumeAnterior.multiply(LIMITE_REGRA_DEZ_POR_CENTO);
        return volumeAtual.compareTo(limiteAceito) > 0;
    }

    public String gerarResumo() {
        return String.format(
                Locale.ROOT,
                "Semana %s iniciada em %s: volume=%s km, paceMedio=%s, adesao=%.2f%%, treinos=%d.",
                numeroSemana == null ? "-" : numeroSemana,
                dataInicioSemana == null ? "-" : dataInicioSemana,
                formatarDecimal(volumeKm),
                formatarDecimal(paceMedio),
                calcularTaxaAdesao(),
                totalTreinos()
        );
    }

    private BigDecimal volumeValido(ProgressoSemanal progressoSemanal) {
        if (progressoSemanal == null || progressoSemanal.getVolumeKm() == null) {
            return null;
        }
        return progressoSemanal.getVolumeKm();
    }

    private BigDecimal volumeAtualOuZero() {
        return volumeKm == null ? BigDecimal.ZERO : volumeKm;
    }

    private int inteiroOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private String formatarDecimal(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
