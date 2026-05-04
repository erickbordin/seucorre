package com.seucorre.usuario.infrastructure;

import com.seucorre.usuario.domain.CondicaoSaude;
import com.seucorre.usuario.domain.DispositivoExterno;
import com.seucorre.usuario.domain.PerfilCorrida;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<PerfilCorrida> findPerfilCorridaByUsuarioId(UUID usuarioId) {
        return entityManager.createQuery(
                        "select p from PerfilCorrida p where p.usuario.id = :usuarioId",
                        PerfilCorrida.class
                )
                .setParameter("usuarioId", usuarioId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public PerfilCorrida savePerfilCorrida(PerfilCorrida perfilCorrida) {
        if (perfilCorrida.getId() == null) {
            entityManager.persist(perfilCorrida);
            return perfilCorrida;
        }
        return entityManager.merge(perfilCorrida);
    }

    @Override
    public void deletePerfilCorridaByUsuarioId(UUID usuarioId) {
        findPerfilCorridaByUsuarioId(usuarioId).ifPresent(entityManager::remove);
    }

    @Override
    public List<CondicaoSaude> findCondicoesSaudeByUsuarioId(UUID usuarioId) {
        return entityManager.createQuery(
                        "select c from CondicaoSaude c where c.usuario.id = :usuarioId",
                        CondicaoSaude.class
                )
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }

    @Override
    public List<DispositivoExterno> findDispositivosByUsuarioId(UUID usuarioId) {
        return entityManager.createQuery(
                        "select d from DispositivoExterno d where d.usuario.id = :usuarioId",
                        DispositivoExterno.class
                )
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }
}
