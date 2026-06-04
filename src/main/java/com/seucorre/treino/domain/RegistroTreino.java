package com.seucorre.treino.domain;

import com.seucorre.shared.domain.enums.StatusTreino;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "registro_treino")
@Data
public class RegistroTreino {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treino_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SessaoTreino sessaoTreino;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conclusao", length = 50)
    private StatusTreino status;

    @Column(name = "distancia_km", precision = 6, scale = 2)
    private BigDecimal distanciaRealKm;

    @Column(name = "duracao_real_min")
    private Integer duracaoRealMin;

    @Column(name = "fc_media")
    private Integer fcMedia;

    @Column(name = "fc_max")
    private Integer fcMaxima;

    @Column(name = "pace_medio")
    private Integer paceMedioReal;

    @Column(name = "esforco_percebido")
    private Integer esforcoPercebido;

    private Boolean sentiuDor = false;

    @Column(name = "local_dor", length = 100)
    private String localDor;

    private Boolean doente = false;

    private Boolean viagem = false;

    private String observacao;

    public boolean foiConcluido() {
        return status == StatusTreino.CONCLUIDO;
    }

    public boolean foiParcial() {
        return status == StatusTreino.PARCIAL;
    }

    public boolean temAlertaDeSaude() {
        return Boolean.TRUE.equals(sentiuDor) || Boolean.TRUE.equals(doente);
    }

    public double calcularDesvioDistancia(BigDecimal planejado) {
        if (planejado == null || planejado.signum() == 0 || distanciaRealKm == null) {
            return 0d;
        }
        return distanciaRealKm.subtract(planejado)
                .divide(planejado, 4, java.math.RoundingMode.HALF_UP)
                .doubleValue() * 100d;
    }

    public double calcularDesvioPace(BigDecimal paceAlvoPlanejado) {
        if (paceAlvoPlanejado == null || paceMedioReal == null) {
            return 0d;
        }
        return BigDecimal.valueOf(paceMedioReal)
                .subtract(paceAlvoPlanejado)
                .doubleValue();
    }

    public String gerarResumoParaIA() {
        List<String> partes = new ArrayList<>();
        if (status != null) {
            partes.add("status=" + status.name());
        }
        if (distanciaRealKm != null) {
            partes.add("distanciaKm=" + distanciaRealKm);
        }
        if (duracaoRealMin != null) {
            partes.add("duracaoRealMin=" + duracaoRealMin);
        }
        if (fcMedia != null) {
            partes.add("fcMedia=" + fcMedia);
        }
        if (paceMedioReal != null) {
            partes.add("paceMedio=" + paceMedioReal);
        }
        if (temAlertaDeSaude()) {
            partes.add("alertaSaude=true");
        }
        if (Boolean.TRUE.equals(viagem)) {
            partes.add("viagem=true");
        }
        if (observacao != null && !observacao.isBlank()) {
            partes.add("observacao=" + observacao.trim());
        }
        return String.join(", ", partes);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
