package com.seucorre.treino.infrastructure;

import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.treino.domain.RegistroTreino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegistroRepository extends JpaRepository<RegistroTreino, UUID> {

    List<RegistroTreino> findByStatus(StatusTreino status);

    List<RegistroTreino> findBySessaoTreinoId(UUID treinoId);
}
