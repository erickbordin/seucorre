package com.seucorre.avaliacao.infrastructure;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckinRepository extends JpaRepository<CheckinSemanal, UUID> {

    List<CheckinSemanal> findByUsuarioIdOrderBySemanaAsc(UUID usuarioId);

    List<CheckinSemanal> findByPlanoIdOrderBySemanaAsc(UUID planoId);

    Optional<CheckinSemanal> findByPlanoIdAndSemana(UUID planoId, Integer semana);
}
