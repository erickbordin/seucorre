package com.seucorre.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "zona_fc")
@Data
public class ZonaFCPersistida {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_corrida_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PerfilCorrida perfilCorrida;

    private Integer zona;
    private String nome;

    @Column(name = "fc_min")
    private Integer fcMin;

    @Column(name = "fc_max")
    private Integer fcMax;

    private Boolean personalizada = false;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
