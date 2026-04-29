package com.seucorre.shared.domain.valueobjects;

public class IMC {

    private final double valor;

    // Construtor privado: obriga o uso do factory method
    private IMC(double valor) {
        this.valor = valor;
    }

    /**
     * Factory Method para instanciar e calcular o IMC.
     */
    public static IMC calcular(double peso, double altura) {
        if (!Double.isFinite(peso) || !Double.isFinite(altura)) {
            throw new IllegalArgumentException("Peso e altura devem ser valores finitos.");
        }
        if (peso <= 0 || altura <= 0) {
            throw new IllegalArgumentException("Peso e altura devem ser maiores que zero.");
        }
        
        double valorCalculado = peso / (altura * altura);
        return new IMC(valorCalculado);
    }

    public double getValor() {
        return this.valor;
    }

    public String getClassificacao() {
        if (valor < 18.5) {
            return "Abaixo do peso";
        } else if (valor < 25.0) {
            return "Peso normal";
        } else if (valor < 30.0) {
            return "Sobrepeso";
        } else {
            return "Obesidade";
        }
    }

    public boolean isNormal() {
        return valor >= 18.5 && valor < 25.0;
    }
}