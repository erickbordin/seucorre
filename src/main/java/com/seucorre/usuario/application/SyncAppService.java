package com.seucorre.usuario.application;

import com.seucorre.avaliacao.application.ProgressoAppService;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.application.dto.RegistroTreinoDTO;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.RegistroRepository;
import com.seucorre.usuario.application.dto.DispositivoExternoDTO;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.SincronizacaoDispositivoDTO;
import com.seucorre.usuario.domain.DispositivoExterno;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyncAppService {

    private final UsuarioRepository usuarioRepository;
    private final RegistroRepository registroRepository;
    private final ProgressoAppService progressoAppService;
    private final WearablePlatformRegistry wearablePlatformRegistry;

    @Transactional(readOnly = true)
    public List<DispositivoExternoDTO> listarDispositivos(UUID usuarioId) {
        validarUsuarioId(usuarioId);
        return buscarUsuarioOuFalhar(usuarioId).getDispositivos().stream()
                .sorted(Comparator.comparing(dispositivoExterno -> dispositivoExterno.getPlataforma().name()))
                .map(dispositivoExterno -> DispositivoExternoDTO.from(dispositivoExterno, wearablePlatformRegistry.suporta(dispositivoExterno.getPlataforma())))
                .toList();
    }

    @Transactional
    public DispositivoExternoDTO vincularDispositivo(UUID usuarioId, DispositivoExternoRequest request) {
        validarUsuarioId(usuarioId);
        if (request == null) {
            throw new IllegalArgumentException("Dados do dispositivo são obrigatórios.");
        }
        validarPlataformaSuportada(request.plataforma());

        Usuario usuario = buscarUsuarioOuFalhar(usuarioId);
        DispositivoExterno dispositivoExterno = usuario.getDispositivos().stream()
                .filter(item -> request.plataforma() == item.getPlataforma())
                .findFirst()
                .orElseGet(() -> {
                    DispositivoExterno novoDispositivo = new DispositivoExterno();
                    novoDispositivo.setPlataforma(request.plataforma());
                    usuario.vincularDispositivo(novoDispositivo);
                    return novoDispositivo;
                });

        dispositivoExterno.setPlataforma(request.plataforma());
        dispositivoExterno.renovarToken(request.tokenAcesso().trim());
        dispositivoExterno.setTokenExpiresAt(request.tokenExpiresAt());

        Usuario usuarioPersistido = usuarioRepository.save(usuario);
        DispositivoExterno dispositivoPersistido = usuarioPersistido.getDispositivos().stream()
                .filter(item -> request.plataforma() == item.getPlataforma())
                .findFirst()
                .orElse(dispositivoExterno);

        return DispositivoExternoDTO.from(dispositivoPersistido, wearablePlatformRegistry.suporta(request.plataforma()));
    }

    @Transactional
    public SincronizacaoDispositivoDTO sincronizarDispositivo(UUID usuarioId, PlataformaRelogio plataforma) {
        return SincronizacaoDispositivoDTO.from(plataforma, sincronizarDados(usuarioId, plataforma));
    }

    @Transactional
    public List<RegistroTreinoDTO> sincronizarDados(UUID usuarioId, PlataformaRelogio plataforma) {
        validarUsuarioId(usuarioId);
        if (plataforma == null) {
            throw new IllegalArgumentException("Plataforma é obrigatória.");
        }
        validarPlataformaSuportada(plataforma);

        Usuario usuario = buscarUsuarioOuFalhar(usuarioId);
        DispositivoExterno dispositivoExterno = localizarDispositivo(usuario, plataforma);
        validarToken(dispositivoExterno);

        WearableAdapter wearableAdapter = wearablePlatformRegistry.localizarAdapter(plataforma)
                .orElseThrow(() -> new BusinessRuleException("Nenhum adapter disponível para a plataforma informada."));
        List<WearableAdapter.AtividadeExterna> atividadesExternas = wearableAdapter.sincronizarDados(dispositivoExterno);
        if (atividadesExternas == null || atividadesExternas.isEmpty()) {
            return List.of();
        }

        List<SessaoTreino> sessoesPendentes = registroRepository.findSessoesByUsuarioIdOrderByDataPrevistaDesc(usuarioId).stream()
                .filter(sessaoTreino -> !sessaoTreino.foiExecutada())
                .sorted(Comparator.comparing(SessaoTreino::getDataPrevista, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<UUID, SessaoTreino> sessoesDisponiveis = sessoesPendentes.stream()
                .collect(Collectors.toMap(SessaoTreino::getId, Function.identity(), (primeira, segunda) -> primeira));

        return atividadesExternas.stream()
                .sorted(Comparator.comparing(WearableAdapter.AtividadeExterna::dataRealizacao, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(atividadeExterna -> importarAtividade(usuarioId, atividadeExterna, sessoesDisponiveis))
                .filter(java.util.Objects::nonNull)
                .map(RegistroTreinoDTO::from)
                .toList();
    }

    private RegistroTreino importarAtividade(UUID usuarioId,
                                             WearableAdapter.AtividadeExterna atividadeExterna,
                                             Map<UUID, SessaoTreino> sessoesDisponiveis) {
        if (atividadeExterna == null || atividadeExterna.dataRealizacao() == null) {
            return null;
        }

        SessaoTreino sessaoTreino = localizarSessaoCorrespondente(usuarioId, atividadeExterna, sessoesDisponiveis);
        if (sessaoTreino == null) {
            return null;
        }

        RegistroTreino registroTreino = toRegistroTreino(atividadeExterna);
        sessaoTreino.registrarExecucao(registroTreino);
        registroTreino.calcularDesvioPace(sessaoTreino.getPaceAlvo());
        registroTreino.calcularDesvioDistancia(sessaoTreino.getDistanciaKm());
        registroTreino.temAlertaDeSaude();

        RegistroTreino registroSalvo = registroRepository.save(registroTreino);
        progressoAppService.atualizarProgressoSemanal(registroSalvo);
        sessoesDisponiveis.remove(sessaoTreino.getId());
        return registroSalvo;
    }

    private SessaoTreino localizarSessaoCorrespondente(UUID usuarioId,
                                                       WearableAdapter.AtividadeExterna atividadeExterna,
                                                       Map<UUID, SessaoTreino> sessoesDisponiveis) {
        return sessoesDisponiveis.values().stream()
                .filter(sessaoTreino -> sessaoTreino.getPlano() != null
                        && sessaoTreino.getPlano().getUsuario() != null
                        && usuarioId.equals(sessaoTreino.getPlano().getUsuario().getId()))
                .filter(sessaoTreino -> atividadeExterna.dataRealizacao().equals(sessaoTreino.getDataPrevista()))
                .findFirst()
                .orElse(null);
    }

    private RegistroTreino toRegistroTreino(WearableAdapter.AtividadeExterna atividadeExterna) {
        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setStatus(atividadeExterna.status() == null ? StatusTreino.CONCLUIDO : atividadeExterna.status());
        registroTreino.setDistanciaRealKm(atividadeExterna.distanciaKm());
        registroTreino.setDuracaoRealMin(atividadeExterna.duracaoRealMin());
        registroTreino.setFcMedia(atividadeExterna.fcMedia());
        registroTreino.setFcMaxima(atividadeExterna.fcMaxima());
        registroTreino.setPaceMedioReal(atividadeExterna.paceMedio());
        registroTreino.setEsforcoPercebido(atividadeExterna.esforcoPercebido());
        registroTreino.setSentiuDor(Boolean.TRUE.equals(atividadeExterna.sentiuDor()));
        registroTreino.setDoente(Boolean.TRUE.equals(atividadeExterna.doente()));
        registroTreino.setObservacao(atividadeExterna.observacao());
        return registroTreino;
    }

    private DispositivoExterno localizarDispositivo(Usuario usuario, PlataformaRelogio plataforma) {
        return usuario.getDispositivos().stream()
                .filter(dispositivoExterno -> plataforma == dispositivoExterno.getPlataforma())
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Dispositivo não vinculado para a plataforma informada."));
    }

    private void validarToken(DispositivoExterno dispositivoExterno) {
        if (!dispositivoExterno.tokenValido()) {
            throw new BusinessRuleException("O dispositivo externo precisa de um token válido para sincronizar dados.");
        }
    }

    private Usuario buscarUsuarioOuFalhar(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    private void validarUsuarioId(UUID usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Usuário é obrigatório.");
        }
    }

    private void validarPlataformaSuportada(PlataformaRelogio plataforma) {
        if (!wearablePlatformRegistry.suporta(plataforma)) {
            throw new BusinessRuleException(wearablePlatformRegistry.mensagemPlataformaNaoDisponivel(plataforma));
        }
    }
}
