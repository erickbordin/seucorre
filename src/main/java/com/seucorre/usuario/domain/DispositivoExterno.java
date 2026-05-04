package com.seucorre.usuario.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "relogio")
@Data
public class DispositivoExterno {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlataformaRelogio plataforma;

    @JsonIgnore
    @Column(name = "token_acesso", length = 1000)
    private String tokenAcesso;

    @JsonIgnore
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    public boolean tokenValido() {
        return tokenAcesso != null
                && tokenExpiresAt != null
                && tokenExpiresAt.isAfter(LocalDateTime.now());
    }

    public void renovarToken(String tokenAcesso) {
        this.tokenAcesso = tokenAcesso;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
