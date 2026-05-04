package com.seucorre.avaliacao.domain;

import com.seucorre.shared.domain.enums.NivelRisco;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.usuario.domain.Usuario;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checkin_semanal")
@Data
public class CheckinSemanal {

    private static final int ESFORCO_SOBRECARGA = 8;
    private static final int ESFORCO_ALTO = 9;
    private static final int DOR_MODERADA = 4;
    private static final int DOR_ALTA = 7;
    private static final int DOR_CRITICA = 9;
    private static final int SONO_BAIXO = 6;
    private static final int SONO_CRITICO = 4;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PlanoTreino plano;

    @Column(length = 100)
    private String avaliacao;

    @Column(name = "data_checkin")
    private LocalDate dataCheckin;

    @Column(name = "nivel_esforco")
    private Integer nivelEsforco;

    @Column(name = "nivel_dor")
    private Integer nivelDor;

    @Column(name = "horas_sono_semana")
    private Integer horasSonoSemana;

    @Column(name = "analise_ia")
    private String analiseIA;

    @Column(name = "plano_reescrito")
    private Boolean planoReescrito = false;

    public boolean indicaSobrecarga() {
        int esforco = inteiroOuZero(nivelEsforco);
        int sono = inteiroOuZero(horasSonoSemana);
        return esforco >= ESFORCO_ALTO || (esforco >= ESFORCO_SOBRECARGA && sono <= SONO_BAIXO);
    }

    public boolean indicaRecuperacaoInsuficiente() {
        int dor = inteiroOuZero(nivelDor);
        int sono = inteiroOuZero(horasSonoSemana);
        return dor >= DOR_MODERADA || sono <= SONO_BAIXO;
    }

    public NivelRisco avaliarNivelRisco() {
        int dor = inteiroOuZero(nivelDor);
        int esforco = inteiroOuZero(nivelEsforco);
        int sono = inteiroOuZero(horasSonoSemana);

        if (dor >= DOR_CRITICA || (dor >= DOR_ALTA && sono <= SONO_CRITICO)) {
            return NivelRisco.CRITICO;
        }
        if (dor >= DOR_ALTA || (esforco >= ESFORCO_SOBRECARGA && sono <= SONO_CRITICO)) {
            return NivelRisco.ALTO;
        }
        if (indicaSobrecarga() || indicaRecuperacaoInsuficiente()) {
            return NivelRisco.MODERADO;
        }
        return NivelRisco.BAIXO;
    }

    public boolean precisaReescreverPlano() {
        return avaliarNivelRisco() != NivelRisco.BAIXO;
    }

    public String gerarContextoParaIA() {
        List<String> partes = new ArrayList<>();
        if (dataCheckin != null) {
            partes.add("dataCheckin=" + dataCheckin);
        }
        if (nivelEsforco != null) {
            partes.add("nivelEsforco=" + nivelEsforco);
        }
        if (nivelDor != null) {
            partes.add("nivelDor=" + nivelDor);
        }
        if (horasSonoSemana != null) {
            partes.add("horasSonoSemana=" + horasSonoSemana);
        }
        if (avaliacao != null && !avaliacao.isBlank()) {
            partes.add("avaliacao=" + avaliacao.trim());
        }
        partes.add("risco=" + avaliarNivelRisco().name());
        partes.add("sobrecarga=" + indicaSobrecarga());
        partes.add("recuperacaoInsuficiente=" + indicaRecuperacaoInsuficiente());
        return String.join(", ", partes);
    }

    private int inteiroOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (planoReescrito == null) {
            planoReescrito = false;
        }
    }
}
