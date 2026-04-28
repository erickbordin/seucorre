package com.seucorre.shared.util;

import java.util.UUID;

public class UuidGenerator {

    private UuidGenerator() {
        throw new UnsupportedOperationException("Classe utilitária.");
    }

    public static UUID generate() {
        return UUID.randomUUID();
    }
}