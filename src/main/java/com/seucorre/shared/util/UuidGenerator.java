package com.seucorre.shared.util;

import java.util.UUID;

public class UuidGenerator {

    // Excelente prática! Mantivemos o construtor privado.
    private UuidGenerator() {
        throw new UnsupportedOperationException("Classe utilitária.");
    }

    // Método 1: Retorna o UUID como Objeto (O teste 'deveGerarUuidTipadoValido' chama esse)
    public static UUID generateUuid() {
        return UUID.randomUUID();
    }

    // Método 2: Retorna o UUID como Texto (O teste 'deveGerarStringValidaNoFormatoUuid' chama esse)
    public static String generateString() {
        return UUID.randomUUID().toString();
    }
}