package com.seucorre.treino.application;

import com.seucorre.infra.cache.CacheConfig;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.application.dto.GerarPlanoRequest;
import com.seucorre.treino.application.dto.PlanoTreinoDTO;
import com.seucorre.treino.application.dto.SessaoTreinoDTO;
import com.seucorre.treino.domain.GeradorPlanoIA;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.infrastructure.PlanoRepository;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoAppService {

    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeradorPlanoIA geradorPlanoIA;

    @Transactional
    @CachePut(cacheNames = CacheConfig.PLANO_ATIVO_CACHE, key = "#usuarioId")
    public PlanoTreinoDTO criarPlano(UUID usuarioId, GerarPlanoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de geração de plano é obrigatório.");
        }

        Usuario usuario = carregarPerfilCompleto(usuarioId);
        PlanoTreino planoTreino = geradorPlanoIA.gerarPlano(usuario, request.objetivo());
        PlanoTreino planoSalvo = planoRepository.save(planoTreino);
        return PlanoTreinoDTO.from(planoSalvo);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.PLANO_ATIVO_CACHE, key = "#usuarioId")
    public PlanoTreinoDTO buscarPlanoAtivo(UUID usuarioId) {
        PlanoTreino planoTreino = planoRepository.findFirstByUsuarioIdAndStatusOrderByDataInicioDesc(usuarioId, StatusPlano.ATIVO)
                .orElseThrow(() -> new EntityNotFoundException("Plano de treino ativo não encontrado."));
        return PlanoTreinoDTO.from(planoTreino);
    }

    @Transactional(readOnly = true)
    public PlanoTreinoDTO buscarPlano(UUID planoId) {
        return PlanoTreinoDTO.from(buscarPlanoOuFalhar(planoId));
    }

    @Transactional(readOnly = true)
    public PlanoTreinoDTO buscarPlano(UUID usuarioId, UUID planoId) {
        return PlanoTreinoDTO.from(buscarPlanoDoUsuarioOuFalhar(usuarioId, planoId));
    }

    @Transactional(readOnly = true)
    public List<PlanoTreinoDTO> listarPlanosDoUsuario(UUID usuarioId) {
        return planoRepository.findByUsuarioId(usuarioId).stream()
                .map(PlanoTreinoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessaoTreinoDTO> listarSessoesDaSemana(UUID usuarioId, Integer semana) {
        return planoRepository.findSessoesByUsuarioIdAndNumeroSemana(usuarioId, semana).stream()
                .map(SessaoTreinoDTO::from)
                .toList();
    }

    @Transactional
    public PlanoTreinoDTO pausarPlano(UUID planoId) {
        PlanoTreino planoTreino = buscarPlanoOuFalhar(planoId);
        planoTreino.pausar();
        return PlanoTreinoDTO.from(planoRepository.save(planoTreino));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PLANO_ATIVO_CACHE, key = "#usuarioId")
    public PlanoTreinoDTO pausarPlano(UUID usuarioId, UUID planoId) {
        PlanoTreino planoTreino = buscarPlanoDoUsuarioOuFalhar(usuarioId, planoId);
        planoTreino.pausar();
        return PlanoTreinoDTO.from(planoRepository.save(planoTreino));
    }

    @Transactional
    public PlanoTreinoDTO reativarPlano(UUID planoId) {
        PlanoTreino planoTreino = buscarPlanoOuFalhar(planoId);
        planoTreino.reativar();
        return PlanoTreinoDTO.from(planoRepository.save(planoTreino));
    }

    @Transactional
    @CachePut(cacheNames = CacheConfig.PLANO_ATIVO_CACHE, key = "#usuarioId")
    public PlanoTreinoDTO reativarPlano(UUID usuarioId, UUID planoId) {
        PlanoTreino planoTreino = buscarPlanoDoUsuarioOuFalhar(usuarioId, planoId);
        planoTreino.reativar();
        return PlanoTreinoDTO.from(planoRepository.save(planoTreino));
    }

    private PlanoTreino buscarPlanoOuFalhar(UUID planoId) {
        return planoRepository.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException("Plano de treino não encontrado."));
    }

    private PlanoTreino buscarPlanoDoUsuarioOuFalhar(UUID usuarioId, UUID planoId) {
        PlanoTreino planoTreino = buscarPlanoOuFalhar(planoId);
        if (planoTreino.getUsuario() == null || !usuarioId.equals(planoTreino.getUsuario().getId())) {
            throw new BusinessRuleException("O plano informado não pertence ao usuário.");
        }
        return planoTreino;
    }

    private Usuario carregarPerfilCompleto(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        usuarioRepository.findPerfilCorridaByUsuarioId(usuarioId)
                .ifPresent(perfilCorrida -> {
                    if (usuario.getPerfilAtleta() != null) {
                        usuario.getPerfilAtleta().atualizarPerfilCorrida(perfilCorrida);
                    }
                });

        return usuario;
    }
}
