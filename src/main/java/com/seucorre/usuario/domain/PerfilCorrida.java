package com.seucorre.usuario.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "perfil_corrida")
@Data
public class PerfilCorrida {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @Column(name = "pace_5k_min_km")
    private BigDecimal pace5kMinKm;

    @Column(name = "pace_10k_min_km")
    private BigDecimal pace10kMinKm;

    @Column(name = "pace_21k_min_km")
    private BigDecimal pace21kMinKm;

    @Column(name = "pace_42k_min_km")
    private BigDecimal pace42kMinKm;

    @Column(name = "vo2_estimado")
    private Integer vo2Estimado;

    @OneToMany(mappedBy = "perfilCorrida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ZonaFCPersistida> zonasFc = new ArrayList<>();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public void substituirZonas(List<ZonaFCPersistida> novasZonas) {
        zonasFc.clear();
        if (novasZonas == null) {
            return;
        }
        novasZonas.forEach(this::adicionarZona);
    }

    public void adicionarZona(ZonaFCPersistida zona) {
        zona.setPerfilCorrida(this);
        zonasFc.add(zona);
    }

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
