package com.seucorre.shared.api;

import java.util.List;

public record ApiResponse<T>(
        boolean sucesso,
        String mensagem,
        T dados,
        List<String> erros
) {
    public static <T> ApiResponse<T> sucesso(T dados) {
        return new ApiResponse<>(true, "Operação realizada com sucesso", dados, null);
    }

    public static <T> ApiResponse<T> sucesso(T dados, String mensagem) {
        return new ApiResponse<>(true, mensagem, dados, null);
    }

    public static <T> ApiResponse<T> erro(String mensagem) {
        return new ApiResponse<>(false, mensagem, null, null);
    }

    public static <T> ApiResponse<T> erro(String mensagem, List<String> erros) {
        return new ApiResponse<>(false, mensagem, null, erros);
    }
}