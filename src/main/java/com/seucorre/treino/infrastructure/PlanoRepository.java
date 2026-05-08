package com.seucorre.treino.infrastructure;

import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.SessaoTreino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanoRepository extends JpaRepository<PlanoTreino, UUID> {

    List<PlanoTreino> findByUsuarioId(UUID usuarioId);

    List<PlanoTreino> findByUsuarioIdAndStatus(UUID usuarioId, StatusPlano status);

    List<PlanoTreino> findByStatus(StatusPlano status);

    Optional<PlanoTreino> findFirstByUsuarioIdAndStatusOrderByDataInicioDesc(UUID usuarioId, StatusPlano status);

    @Query("""
            select s
            from SessaoTreino s
            where s.plano.usuario.id = :usuarioId
              and s.numeroSemana = :semana
            order by s.dataPrevista asc
            """)
    List<SessaoTreino> findSessoesByUsuarioIdAndNumeroSemana(@Param("usuarioId") UUID usuarioId,
                                                             @Param("semana") Integer semana);
}
