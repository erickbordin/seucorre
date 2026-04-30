package com.seucorre.usuario.domain;

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

import java.util.UUID;

@Entity
@Table(name = "condicao_saude")
@Data
public class CondicaoSaude {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    private String tipo;

    @Column(length = 500)
    private String descricao;

    private Boolean ativa = true;

    public boolean restringeAtividade() {
        return Boolean.TRUE.equals(ativa) && tipo != null && (
                tipo.equalsIgnoreCase("CARDIACA")
                        || tipo.equalsIgnoreCase("LESAO_GRAVE")
                        || tipo.equalsIgnoreCase("RESPIRATORIA_GRAVE")
        );
    }

    public boolean exigeMonitoramentoFC() {
        return Boolean.TRUE.equals(ativa) && tipo != null && (
                tipo.equalsIgnoreCase("CARDIACA")
                        || tipo.equalsIgnoreCase("HIPERTENSAO")
        );
    }

    public boolean exigeAutorizacaoMedica() {
        return restringeAtividade() || exigeMonitoramentoFC();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (ativa == null) {
            ativa = true;
        }
    }
}
