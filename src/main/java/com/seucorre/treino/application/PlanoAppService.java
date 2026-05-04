package com.seucorre.treino.application;

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
    public PlanoTreinoDTO buscarPlano(UUID planoId) {
        return PlanoTreinoDTO.from(buscarPlanoOuFalhar(planoId));
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
    public PlanoTreinoDTO reativarPlano(UUID planoId) {
        PlanoTreino planoTreino = buscarPlanoOuFalhar(planoId);
        planoTreino.reativar();
        return PlanoTreinoDTO.from(planoRepository.save(planoTreino));
    }

    private PlanoTreino buscarPlanoOuFalhar(UUID planoId) {
        return planoRepository.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException("Plano de treino não encontrado."));
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
