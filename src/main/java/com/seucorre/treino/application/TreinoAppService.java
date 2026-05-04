package com.seucorre.treino.application;

import com.seucorre.avaliacao.application.AvaliacaoAppService;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.application.dto.RegistroTreinoDTO;
import com.seucorre.treino.application.dto.RegistroTreinoRequest;
import com.seucorre.treino.application.dto.SessaoTreinoDTO;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.RegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TreinoAppService {

    private final RegistroRepository registroRepository;
    private final AvaliacaoAppService avaliacaoAppService;

    @Transactional
    public RegistroTreinoDTO registrarExecucao(RegistroTreinoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de registro de treino é obrigatório.");
        }

        SessaoTreino sessaoTreino = registroRepository.findSessaoTreinoById(request.treinoId())
                .orElseThrow(() -> new EntityNotFoundException("Sessão de treino não encontrada."));

        RegistroTreino registroTreino = toRegistroTreino(request);
        sessaoTreino.registrarExecucao(registroTreino);

        // Força o cálculo dos indicadores documentados no fluxo antes da persistência.
        registroTreino.calcularDesvioPace(sessaoTreino.getPaceAlvo());
        registroTreino.calcularDesvioDistancia(sessaoTreino.getDistanciaKm());
        registroTreino.temAlertaDeSaude();

        RegistroTreino registroSalvo = registroRepository.save(registroTreino);
        avaliacaoAppService.atualizarProgressoSemanal(registroSalvo);
        return RegistroTreinoDTO.from(registroSalvo);
    }

    @Transactional(readOnly = true)
    public List<SessaoTreinoDTO> listarHistoricoSessoes(UUID usuarioId) {
        return registroRepository.findSessoesByUsuarioIdOrderByDataPrevistaDesc(usuarioId).stream()
                .map(SessaoTreinoDTO::from)
                .toList();
    }

    private RegistroTreino toRegistroTreino(RegistroTreinoRequest request) {
        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setStatus(request.status());
        registroTreino.setDistanciaRealKm(request.distanciaRealKm());
        registroTreino.setFcMedia(request.fcMedia());
        registroTreino.setFcMaxima(request.fcMaxima());
        registroTreino.setPaceMedioReal(request.paceMedioReal());
        registroTreino.setEsforcoPercebido(request.esforcoPercebido());
        registroTreino.setSentiuDor(Boolean.TRUE.equals(request.sentiuDor()));
        registroTreino.setLocalDor(request.localDor());
        registroTreino.setDoente(Boolean.TRUE.equals(request.doente()));
        registroTreino.setViagem(Boolean.TRUE.equals(request.viagem()));
        registroTreino.setObservacao(request.observacao());
        return registroTreino;
    }
}
