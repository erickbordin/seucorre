package com.seucorre.treino.domain;

import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.usuario.domain.Usuario;
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
@Table(name = "plano_treino")
@Data
public class PlanoTreino {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @Column(name = "total_semanas")
    private Integer totalSemanas;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "km_totais", precision = 7, scale = 2)
    private BigDecimal kmTotais;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StatusPlano status = StatusPlano.ATIVO;

    @Column(name = "resumo_ia")
    private String resumoIA;

    @Column(name = "guia_tipos_treino")
    private String guiaTiposTreino;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SessaoTreino> sessoes = new ArrayList<>();

    public double calcularCargaSemanal(Integer semana) {
        return obterSessoesDaSemana(semana).stream()
                .mapToDouble(SessaoTreino::calcularCarga)
                .sum();
    }

    public double calcularProgressoGeral() {
        if (sessoes == null || sessoes.isEmpty()) {
            return 0d;
        }
        long sessoesExecutadas = sessoes.stream()
                .filter(SessaoTreino::foiExecutada)
                .count();
        return (sessoesExecutadas * 100d) / sessoes.size();
    }

    public List<SessaoTreino> obterSessoesDaSemana(Integer semana) {
        if (semana == null) {
            return List.of();
        }
        return sessoes.stream()
                .filter(sessao -> semana.equals(sessao.getNumeroSemana()))
                .toList();
    }

    public boolean estaNoPrazo() {
        return dataFim == null || !dataFim.isBefore(LocalDate.now());
    }

    public boolean verificarRegra10Porcento(Integer semana) {
        if (semana == null || semana <= 1) {
            return true;
        }
        double cargaSemanaAtual = calcularCargaSemanal(semana);
        double cargaSemanaAnterior = calcularCargaSemanal(semana - 1);
        if (cargaSemanaAnterior <= 0) {
            return true;
        }
        return cargaSemanaAtual <= cargaSemanaAnterior * 1.10d;
    }

    public void pausar() {
        status = StatusPlano.PAUSADO;
    }

    public void reativar() {
        status = StatusPlano.ATIVO;
    }

    public void concluir() {
        status = StatusPlano.CONCLUIDO;
    }

    public void adicionarSessao(SessaoTreino sessaoTreino) {
        if (sessaoTreino == null) {
            return;
        }
        sessaoTreino.setPlano(this);
        sessoes.add(sessaoTreino);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = StatusPlano.ATIVO;
        }
    }
}
