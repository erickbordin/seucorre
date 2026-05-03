package com.seucorre.usuario.infrastructure;

import com.seucorre.usuario.domain.PerfilCorrida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PerfilCorridaRepository extends JpaRepository<PerfilCorrida, UUID> {

    Optional<PerfilCorrida> findByUsuarioId(UUID usuarioId);
}
