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

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "condicao_saude")
@Data
public class CondicaoSaude {

    private static final Set<String> TIPOS_QUE_RESTRINGEM_ATIVIDADE = Set.of(
            "CARDIACA",
            "LESAO_GRAVE",
            "RESPIRATORIA_GRAVE"
    );

    private static final Set<String> TIPOS_QUE_EXIGEM_MONITORAMENTO_FC = Set.of(
            "CARDIACA",
            "HIPERTENSAO"
    );

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativa = true;

    public boolean restringeAtividade() {
        return estaAtiva() && TIPOS_QUE_RESTRINGEM_ATIVIDADE.contains(tipoNormalizado());
    }

    public boolean exigeMonitoramentoFC() {
        return estaAtiva() && TIPOS_QUE_EXIGEM_MONITORAMENTO_FC.contains(tipoNormalizado());
    }

    public boolean exigeAutorizacaoMedica() {
        return restringeAtividade() || exigeMonitoramentoFC();
    }

    private boolean estaAtiva() {
        return Boolean.TRUE.equals(ativa);
    }

    private String tipoNormalizado() {
        if (tipo == null) {
            return "";
        }
        return tipo.trim().toUpperCase(Locale.ROOT);
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
