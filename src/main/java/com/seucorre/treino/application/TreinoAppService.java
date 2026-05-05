package com.seucorre.treino.application;

import com.seucorre.avaliacao.application.ProgressoAppService;
import com.seucorre.shared.exception.BusinessRuleException;
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
    private final ProgressoAppService progressoAppService;

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
        progressoAppService.atualizarProgressoSemanal(registroSalvo);
        return RegistroTreinoDTO.from(registroSalvo);
    }

    @Transactional
    public RegistroTreinoDTO registrarExecucao(UUID usuarioId, UUID treinoId, RegistroTreinoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de registro de treino é obrigatório.");
        }
        if (!treinoId.equals(request.treinoId())) {
            throw new BusinessRuleException("O treino informado no corpo da requisição difere do identificador da URL.");
        }

        SessaoTreino sessaoTreino = buscarSessaoDoUsuarioOuFalhar(usuarioId, treinoId);
        RegistroTreino registroTreino = toRegistroTreino(request);
        sessaoTreino.registrarExecucao(registroTreino);

        registroTreino.calcularDesvioPace(sessaoTreino.getPaceAlvo());
        registroTreino.calcularDesvioDistancia(sessaoTreino.getDistanciaKm());
        registroTreino.temAlertaDeSaude();

        RegistroTreino registroSalvo = registroRepository.save(registroTreino);
        progressoAppService.atualizarProgressoSemanal(registroSalvo);
        return RegistroTreinoDTO.from(registroSalvo);
    }

    @Transactional(readOnly = true)
    public List<SessaoTreinoDTO> listarSessoesDaSemana(UUID usuarioId, Integer semana) {
        return registroRepository.findSessoesByUsuarioIdAndNumeroSemana(usuarioId, semana).stream()
                .map(SessaoTreinoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessaoTreinoDTO> listarHistoricoSessoes(UUID usuarioId) {
        return registroRepository.findSessoesByUsuarioIdOrderByDataPrevistaDesc(usuarioId).stream()
                .map(SessaoTreinoDTO::from)
                .toList();
    }

    private SessaoTreino buscarSessaoDoUsuarioOuFalhar(UUID usuarioId, UUID treinoId) {
        SessaoTreino sessaoTreino = registroRepository.findSessaoTreinoById(treinoId)
                .orElseThrow(() -> new EntityNotFoundException("Sessão de treino não encontrada."));

        if (sessaoTreino.getPlano() == null
                || sessaoTreino.getPlano().getUsuario() == null
                || !usuarioId.equals(sessaoTreino.getPlano().getUsuario().getId())) {
            throw new BusinessRuleException("O treino informado não pertence ao usuário.");
        }

        return sessaoTreino;
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
