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
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
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
    private final List<WearableAdapter> wearableAdapters;

    @Transactional
    public DispositivoSincronizadoDTO vincularDispositivo(UUID usuarioId, DispositivoExternoRequest request) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Usuário é obrigatório.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request de dispositivo é obrigatória.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        DispositivoExterno dispositivoExterno = usuario.getDispositivos().stream()
                .filter(dispositivo -> request.plataforma() == dispositivo.getPlataforma())
                .findFirst()
                .orElseGet(() -> criarDispositivo(usuario, request.plataforma()));

        dispositivoExterno.setTokenAcesso(request.tokenAcesso());
        dispositivoExterno.setTokenExpiresAt(request.tokenExpiresAt());

        usuarioRepository.save(usuario);
        return DispositivoSincronizadoDTO.from(dispositivoExterno);
    }

    @Transactional
    public List<RegistroTreinoDTO> sincronizarDados(UUID usuarioId, PlataformaRelogio plataforma) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Usuário é obrigatório.");
        }
        if (plataforma == null) {
            throw new IllegalArgumentException("Plataforma é obrigatória.");
        }
        validarPlataformaSuportada(plataforma);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        DispositivoExterno dispositivoExterno = localizarDispositivo(usuario, plataforma);
        validarToken(dispositivoExterno);

        WearableAdapter wearableAdapter = localizarAdapter(plataforma);
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

    @Transactional
    public void removerDispositivo(UUID usuarioId, PlataformaRelogio plataforma) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Usuário é obrigatório.");
        }
        if (plataforma == null) {
            throw new IllegalArgumentException("Plataforma é obrigatória.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        boolean removido = usuario.getDispositivos().removeIf(dispositivo -> plataforma == dispositivo.getPlataforma());
        if (!removido) {
            throw new EntityNotFoundException("Dispositivo não vinculado para a plataforma informada.");
        }

        usuarioRepository.save(usuario);
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

    private WearableAdapter localizarAdapter(PlataformaRelogio plataforma) {
        return wearableAdapters.stream()
                .filter(wearableAdapter -> plataforma == wearableAdapter.plataformaSuportada())
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Nenhum adapter disponível para a plataforma informada."));
    }

    private void validarToken(DispositivoExterno dispositivoExterno) {
        if (!dispositivoExterno.tokenValido()) {
            throw new BusinessRuleException("O dispositivo externo precisa de um token válido para sincronizar dados.");
        }
    }

    private void validarPlataformaSuportada(PlataformaRelogio plataforma) {
        if (plataforma != PlataformaRelogio.GARMIN && plataforma != PlataformaRelogio.STRAVA) {
            throw new BusinessRuleException("A sincronização suporta apenas Garmin e Strava nesta versão.");
        }
    }

    private DispositivoExterno criarDispositivo(Usuario usuario, PlataformaRelogio plataforma) {
        DispositivoExterno dispositivoExterno = new DispositivoExterno();
        dispositivoExterno.setUsuario(usuario);
        dispositivoExterno.setPlataforma(plataforma);
        usuario.vincularDispositivo(dispositivoExterno);
        return dispositivoExterno;
    }

    public record DispositivoSincronizadoDTO(
            PlataformaRelogio plataforma,
            boolean tokenValido,
            java.time.LocalDateTime tokenExpiresAt
    ) {
        public static DispositivoSincronizadoDTO from(DispositivoExterno dispositivoExterno) {
            return new DispositivoSincronizadoDTO(
                    dispositivoExterno.getPlataforma(),
                    dispositivoExterno.tokenValido(),
                    dispositivoExterno.getTokenExpiresAt()
            );
        }
    }
}
