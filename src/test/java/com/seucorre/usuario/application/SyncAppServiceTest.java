package com.seucorre.usuario.application;

import com.seucorre.avaliacao.application.ProgressoAppService;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.application.dto.RegistroTreinoDTO;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.RegistroRepository;
import com.seucorre.usuario.domain.DispositivoExterno;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncAppServiceTest {

    private UsuarioRepository usuarioRepository;
    private RegistroRepository registroRepository;
    private ProgressoAppService progressoAppService;
    private WearableAdapter garminAdapter;
    private SyncAppService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        registroRepository = mock(RegistroRepository.class);
        progressoAppService = mock(ProgressoAppService.class);
        garminAdapter = mock(WearableAdapter.class);

        when(garminAdapter.plataformaSuportada()).thenReturn(PlataformaRelogio.GARMIN);

        service = new SyncAppService(
                usuarioRepository,
                registroRepository,
                progressoAppService,
                List.of(garminAdapter)
        );
    }

    @Test
    void sincronizaAtividadeExternaParaRegistroTreinoEAtualizaProgresso() {
        Usuario usuario = criarUsuarioComDispositivo(PlataformaRelogio.GARMIN, true);
        SessaoTreino sessaoTreino = criarSessao(usuario, LocalDate.of(2026, 5, 10));

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(registroRepository.findSessoesByUsuarioIdOrderByDataPrevistaDesc(usuario.getId()))
                .thenReturn(List.of(sessaoTreino));
        when(garminAdapter.sincronizarDados(usuario.getDispositivos().get(0)))
                .thenReturn(List.of(new WearableAdapter.AtividadeExterna(
                        LocalDate.of(2026, 5, 10),
                        StatusTreino.CONCLUIDO,
                        new BigDecimal("8.40"),
                        150,
                        176,
                        345,
                        7,
                        true,
                        false,
                        "Importado do Garmin"
                )));
        when(registroRepository.save(any(RegistroTreino.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<RegistroTreinoDTO> registros = service.sincronizarDados(usuario.getId(), PlataformaRelogio.GARMIN);

        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).status()).isEqualTo(StatusTreino.CONCLUIDO);
        assertThat(registros.get(0).distanciaRealKm()).isEqualByComparingTo("8.40");
        assertThat(registros.get(0).alertaSaude()).isTrue();
        verify(progressoAppService).atualizarProgressoSemanal(any(RegistroTreino.class));
    }

    @Test
    void ignoraAtividadeSemSessaoPendenteNaMesmaData() {
        Usuario usuario = criarUsuarioComDispositivo(PlataformaRelogio.GARMIN, true);
        SessaoTreino sessaoTreino = criarSessao(usuario, LocalDate.of(2026, 5, 11));

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(registroRepository.findSessoesByUsuarioIdOrderByDataPrevistaDesc(usuario.getId()))
                .thenReturn(List.of(sessaoTreino));
        when(garminAdapter.sincronizarDados(usuario.getDispositivos().get(0)))
                .thenReturn(List.of(new WearableAdapter.AtividadeExterna(
                        LocalDate.of(2026, 5, 10),
                        StatusTreino.CONCLUIDO,
                        new BigDecimal("8.40"),
                        150,
                        176,
                        345,
                        7,
                        false,
                        false,
                        "Sem sessao correspondente"
                )));

        List<RegistroTreinoDTO> registros = service.sincronizarDados(usuario.getId(), PlataformaRelogio.GARMIN);

        assertThat(registros).isEmpty();
    }

    @Test
    void falhaQuandoDispositivoNaoTemTokenValido() {
        Usuario usuario = criarUsuarioComDispositivo(PlataformaRelogio.GARMIN, false);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.sincronizarDados(usuario.getId(), PlataformaRelogio.GARMIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O dispositivo externo precisa de um token válido para sincronizar dados.");
    }

    @Test
    void falhaQuandoDispositivoNaoEstaVinculado() {
        Usuario usuario = criarUsuarioComDispositivo(PlataformaRelogio.STRAVA, true);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.sincronizarDados(usuario.getId(), PlataformaRelogio.GARMIN))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Dispositivo não vinculado para a plataforma informada.");
    }

    private Usuario criarUsuarioComDispositivo(PlataformaRelogio plataformaRelogio, boolean tokenValido) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());

        DispositivoExterno dispositivoExterno = new DispositivoExterno();
        dispositivoExterno.setPlataforma(plataformaRelogio);
        dispositivoExterno.setTokenAcesso("token-valido");
        dispositivoExterno.setTokenExpiresAt(tokenValido ? LocalDateTime.now().plusDays(2) : LocalDateTime.now().minusMinutes(1));
        usuario.vincularDispositivo(dispositivoExterno);
        return usuario;
    }

    private SessaoTreino criarSessao(Usuario usuario, LocalDate dataPrevista) {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());
        planoTreino.setUsuario(usuario);

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setId(UUID.randomUUID());
        sessaoTreino.setPlano(planoTreino);
        sessaoTreino.setNumeroSemana(1);
        sessaoTreino.setDataPrevista(dataPrevista);
        sessaoTreino.setDistanciaKm(new BigDecimal("8.00"));
        sessaoTreino.setPaceAlvo(new BigDecimal("340.00"));
        return sessaoTreino;
    }
}
