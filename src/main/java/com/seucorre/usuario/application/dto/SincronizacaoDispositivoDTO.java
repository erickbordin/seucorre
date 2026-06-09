package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.treino.application.dto.RegistroTreinoDTO;

import java.util.List;

public record SincronizacaoDispositivoDTO(
        PlataformaRelogio plataforma,
        int registrosImportados,
        List<RegistroTreinoDTO> registros
) {

    public static SincronizacaoDispositivoDTO from(PlataformaRelogio plataforma, List<RegistroTreinoDTO> registros) {
        List<RegistroTreinoDTO> registrosImportados = registros == null ? List.of() : List.copyOf(registros);
        return new SincronizacaoDispositivoDTO(plataforma, registrosImportados.size(), registrosImportados);
    }
}
