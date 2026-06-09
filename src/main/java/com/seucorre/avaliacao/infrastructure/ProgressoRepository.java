package com.seucorre.avaliacao.infrastructure;

import com.seucorre.avaliacao.domain.ProgressoSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgressoRepository extends JpaRepository<ProgressoSemanal, UUID> {

    List<ProgressoSemanal> findByPlanoUsuarioIdOrderByNumeroSemanaAsc(UUID usuarioId);

    List<ProgressoSemanal> findByPlanoIdOrderByNumeroSemanaAsc(UUID planoId);

    List<ProgressoSemanal> findTop3ByPlanoIdOrderByNumeroSemanaDesc(UUID planoId);

    Optional<ProgressoSemanal> findByPlanoIdAndNumeroSemana(UUID planoId, Integer numeroSemana);
}
