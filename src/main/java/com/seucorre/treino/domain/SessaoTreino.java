package com.seucorre.treino.domain;

import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.shared.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "treino")
@Data
public class SessaoTreino {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PlanoTreino plano;

    @Column(name = "numero_semana")
    private Integer numeroSemana;

    @Column(name = "data_prevista")
    private LocalDate dataPrevista;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private TipoTreino tipo;

    @Column(name = "distancia_km", precision = 6, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "duracao_min")
    private Integer duracaoMinutos;

    @Column(length = 50)
    private String intensidade;

    @Column(name = "zona_fc_alvo", length = 50)
    private String zonaFcAlvo;

    @Column(name = "pace_alvo", precision = 5, scale = 2)
    private BigDecimal paceAlvo;

    private String descricao;

    @OneToMany(mappedBy = "sessaoTreino", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RegistroTreino> registros = new ArrayList<>();

    public double calcularCarga() {
        if (tipo == null || distanciaKm == null) {
            return 0d;
        }
        return distanciaKm.doubleValue() * tipo.getFatorIntensidade();
    }

    public boolean foiExecutada() {
        return obterRegistro() != null;
    }

    public void registrarExecucao(RegistroTreino registroTreino) {
        if (registroTreino == null) {
            throw new IllegalArgumentException("Registro de treino é obrigatório.");
        }
        if (foiExecutada()) {
            throw new BusinessRuleException("A sessão de treino já possui registro de execução.");
        }
        registroTreino.setSessaoTreino(this);
        registros.add(registroTreino);
    }

    public boolean estaPendente() {
        return !foiExecutada();
    }

    public boolean estaAtrasada() {
        return dataPrevista != null
                && dataPrevista.isBefore(LocalDate.now())
                && estaPendente();
    }

    public RegistroTreino obterRegistro() {
        return registros.isEmpty() ? null : registros.get(0);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
