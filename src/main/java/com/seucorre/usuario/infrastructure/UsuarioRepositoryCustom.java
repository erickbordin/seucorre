package com.seucorre.usuario.infrastructure;

import com.seucorre.usuario.domain.CondicaoSaude;
import com.seucorre.usuario.domain.DispositivoExterno;
import com.seucorre.usuario.domain.PerfilCorrida;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryCustom {

    Optional<PerfilCorrida> findPerfilCorridaByUsuarioId(UUID usuarioId);

    PerfilCorrida savePerfilCorrida(PerfilCorrida perfilCorrida);

    void deletePerfilCorridaByUsuarioId(UUID usuarioId);

    List<CondicaoSaude> findCondicoesSaudeByUsuarioId(UUID usuarioId);

    List<DispositivoExterno> findDispositivosByUsuarioId(UUID usuarioId);
}
