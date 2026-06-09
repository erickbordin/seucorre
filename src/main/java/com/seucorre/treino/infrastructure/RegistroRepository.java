package com.seucorre.treino.infrastructure;

import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistroRepository extends JpaRepository<RegistroTreino, UUID> {

    List<RegistroTreino> findByStatus(StatusTreino status);

    List<RegistroTreino> findBySessaoTreinoId(UUID treinoId);

    @Query("""
            select s
            from SessaoTreino s
            where s.id = :treinoId
            """)
    Optional<SessaoTreino> findSessaoTreinoById(@Param("treinoId") UUID treinoId);

    @Query("""
            select s
            from SessaoTreino s
            where s.plano.usuario.id = :usuarioId
              and s.numeroSemana = :semana
            order by s.dataPrevista asc
            """)
    List<SessaoTreino> findSessoesByUsuarioIdAndNumeroSemana(@Param("usuarioId") UUID usuarioId,
                                                             @Param("semana") Integer semana);

    @Query("""
            select s
            from SessaoTreino s
            where s.plano.usuario.id = :usuarioId
            order by s.dataPrevista desc
            """)
    List<SessaoTreino> findSessoesByUsuarioIdOrderByDataPrevistaDesc(@Param("usuarioId") UUID usuarioId);

    @Query("""
            select r
            from RegistroTreino r
            where r.sessaoTreino.plano.id = :planoId
              and r.sessaoTreino.numeroSemana = :numeroSemana
            """)
    List<RegistroTreino> findByPlanoIdAndNumeroSemana(@Param("planoId") UUID planoId,
                                                      @Param("numeroSemana") Integer numeroSemana);
}
