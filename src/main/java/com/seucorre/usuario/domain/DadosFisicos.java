package com.seucorre.usuario.domain;

import com.seucorre.shared.domain.valueobjects.IMC;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Embeddable
@Data
public class DadosFisicos implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final double FATOR_UTH_SORENSEN = 15.3d;

    @Column(name = "peso_kg", precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "altura_cm", precision = 5, scale = 2)
    private BigDecimal alturaCm;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "genero", length = 50)
    private String genero;

    @Column(name = "fc_repouso")
    private Integer fcRepouso;

    @Column(name = "fc_maximo")
    private Integer fcMaxima;

    @Column(name = "horas_sono_media")
    private Integer horasSonoMedia;

    @Column(name = "sedentario")
    private Boolean sedentario;

    public IMC calcularIMC() {
        BigDecimal peso = exigirPositivo(pesoKg, "Peso");
        BigDecimal altura = exigirPositivo(alturaCm, "Altura");
        double alturaMetros = altura.divide(CEM, 4, RoundingMode.HALF_UP).doubleValue();
        return IMC.calcular(peso.doubleValue(), alturaMetros);
    }

    public int calcularIdadeCardiaca() {
        return Math.max(0, 220 - obterFcMaximaInformadaOuTeorica());
    }

    public double calcularVo2MaxEstimado() {
        int fcRepousoValidada = exigirFrequenciaPositiva(fcRepouso, "Frequência cardíaca de repouso");
        int fcMaximaValidada = obterFcMaximaInformadaOuTeorica();
        if (fcRepousoValidada >= fcMaximaValidada) {
            throw new IllegalArgumentException("FC de repouso deve ser menor que a FC máxima.");
        }

        // Estimativa clássica de Uth-Sorensen: VO2max = 15.3 * (FCmax / FCrepouso)
        double vo2 = FATOR_UTH_SORENSEN * ((double) fcMaximaValidada / fcRepousoValidada);
        return BigDecimal.valueOf(vo2).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Transient
    public Integer getIdade() {
        if (dataNascimento == null) {
            return null;
        }
        validarDataNascimento(dataNascimento);
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    private int obterFcMaximaInformadaOuTeorica() {
        if (fcMaxima != null) {
            return exigirFrequenciaPositiva(fcMaxima, "Frequência cardíaca máxima");
        }
        Integer idade = getIdade();
        if (idade == null) {
            throw new IllegalStateException("Informe a FC máxima ou a data de nascimento para calcular os indicadores cardíacos.");
        }
        return Math.max(1, 220 - idade);
    }

    private BigDecimal exigirPositivo(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalStateException(campo + " deve ser informado.");
        }
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
        return valor;
    }

    private int exigirFrequenciaPositiva(Integer valor, String campo) {
        if (valor == null) {
            throw new IllegalStateException(campo + " deve ser informada.");
        }
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
        return valor;
    }

    private void validarDataNascimento(LocalDate dataNascimento) {
        if (dataNascimento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de nascimento não pode estar no futuro.");
        }
    }
}
