package com.seucorre.usuario.application;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.shared.domain.valueobjects.IMC;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.usuario.application.dto.CondicaoSaudeRequest;
import com.seucorre.usuario.application.dto.DadosFisicosRequest;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.PerfilAtletaRequest;
import com.seucorre.usuario.application.dto.PerfilCorridaRequest;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.dto.UsuarioResponse;
import com.seucorre.usuario.application.dto.ZonaFCRequest;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.PerfilCorrida;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioAppServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder passwordEncoder;
    private WearableAdapter garminAdapter;
    private WearableAdapter stravaAdapter;
    private UsuarioAppService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        garminAdapter = mock(WearableAdapter.class);
        stravaAdapter = mock(WearableAdapter.class);
        when(garminAdapter.plataformaSuportada()).thenReturn(PlataformaRelogio.GARMIN);
        when(stravaAdapter.plataformaSuportada()).thenReturn(PlataformaRelogio.STRAVA);
        service = new UsuarioAppService(repository, passwordEncoder, new WearablePlatformRegistry(List.of(garminAdapter, stravaAdapter)));
    }

    @Test
    void registrarComOnboardingCompletoCriaUsuarioAptoComSenhaHash() {
        when(repository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.savePerfilCorrida(any(PerfilCorrida.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioCadastroRequest request = cadastroValido("SEG,QUA,SEX");

        UsuarioResponse response = service.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario usuarioSalvo = captor.getValue();

        assertThat(usuarioSalvo.getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(usuarioSalvo.getCondicoesSaude()).hasSize(1);
        assertThat(usuarioSalvo.getDispositivos()).hasSize(1);
        assertThat(response.aptoParaTreinar()).isTrue();
        assertThat(response.perfilCorrida().zonasFc()).hasSize(1);
    }

    @Test
    void registrarSemOnboardingCriaUsuarioBasicoNaoApto() {
        when(repository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioCadastroRequest request = new UsuarioCadastroRequest(
                "Ana Runner",
                "ana@email.com",
                "senha123",
                null,
                null,
                null,
                null,
                null,
                null
        );

        UsuarioResponse response = service.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario usuarioSalvo = captor.getValue();

        assertThat(usuarioSalvo.getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(usuarioSalvo.getDadosFisicos()).isNull();
        assertThat(usuarioSalvo.getPerfilAtleta()).isNull();
        assertThat(response.aptoParaTreinar()).isFalse();
        assertThat(response.perfilCorrida()).isNull();
    }

    @Test
    void registrarRecusaEmailDuplicado() {
        when(repository.existsByEmail("ana@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(cadastroValido("SEG,QUA,SEX")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("E-mail já cadastrado.");
    }

    @Test
    void registrarRecusaDiasDeTreinoAcimaDaDisponibilidade() {
        when(repository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");

        assertThatThrownBy(() -> service.registrar(cadastroValido("SEG,TER,QUA,QUI")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dias de treino não podem exceder os dias disponíveis na semana.");
    }

    @Test
    void registrarRecusaPlataformaSemAdapterConfigurado() {
        when(repository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");

        UsuarioCadastroRequest request = new UsuarioCadastroRequest(
                "Ana Runner",
                "ana@email.com",
                "senha123",
                "11999999999",
                new DadosFisicosRequest(
                        new BigDecimal("62.50"),
                        new BigDecimal("168.00"),
                        LocalDate.of(1995, 5, 10),
                        "FEMININO",
                        62,
                        190,
                        7,
                        false
                ),
                new PerfilAtletaRequest(
                        NivelCondicionamento.INICIANTE,
                        Objetivo.COMPLETAR_5K,
                        true,
                        3,
                        "SEG,QUA,SEX"
                ),
                new PerfilCorridaRequest(
                        new BigDecimal("6.20"),
                        null,
                        null,
                        null,
                        38,
                        List.of(new ZonaFCRequest(1, "Leve", 95, 115, false))
                ),
                List.of(new CondicaoSaudeRequest("ALERGIA", "Rinite", true)),
                List.of(new DispositivoExternoRequest(PlataformaRelogio.POLAR, "token", LocalDateTime.now().plusDays(1)))
        );

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A plataforma Polar Flow ainda não está disponível nesta versão. Plataformas disponíveis: Garmin Connect, Strava.");
    }

    @Test
    void buscarPorIdRetornaPerfilCompletoComPerfilCorrida() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Ana Runner");
        usuario.setEmail("ana@email.com");
        usuario.setPerfilAtleta(new PerfilAtleta());

        PerfilCorrida perfilCorrida = new PerfilCorrida();
        perfilCorrida.setPace5kMinKm(new BigDecimal("5.30"));

        when(repository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(repository.findPerfilCorridaByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilCorrida));

        UsuarioResponse response = service.buscarPorId(usuarioId);

        assertThat(response.id()).isEqualTo(usuarioId);
        assertThat(response.perfilCorrida()).isNotNull();
        assertThat(response.perfilCorrida().pace5kMinKm()).isEqualByComparingTo("5.30");
    }

    @Test
    void buscarPorIdFalhaQuandoUsuarioNaoExiste() {
        UUID usuarioId = UUID.randomUUID();
        when(repository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(usuarioId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Usuário não encontrado.");
    }

    @Test
    void calcularImcRetornaMetricaDoUsuario() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setPesoKg(new BigDecimal("62.50"));
        usuario.setAlturaCm(new BigDecimal("168.00"));

        when(repository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        IMC imc = service.calcularImc(usuarioId);

        assertThat(imc.getValor()).isCloseTo(22.14d, within(0.01d));
        assertThat(imc.getClassificacao()).isEqualTo("Peso normal");
    }

    @Test
    void calcularImcFalhaQuandoDadosFisicosNaoPermitemCalculo() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(repository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.calcularImc(usuarioId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dados físicos não informados.");
    }

    private UsuarioCadastroRequest cadastroValido(String diasSemanaTreino) {
        return new UsuarioCadastroRequest(
                "Ana Runner",
                "ana@email.com",
                "senha123",
                "11999999999",
                new DadosFisicosRequest(
                        new BigDecimal("62.50"),
                        new BigDecimal("168.00"),
                        LocalDate.of(1995, 5, 10),
                        "FEMININO",
                        62,
                        190,
                        7,
                        false
                ),
                new PerfilAtletaRequest(
                        NivelCondicionamento.INICIANTE,
                        Objetivo.COMPLETAR_5K,
                        true,
                        3,
                        diasSemanaTreino
                ),
                new PerfilCorridaRequest(
                        new BigDecimal("6.20"),
                        null,
                        null,
                        null,
                        38,
                        List.of(new ZonaFCRequest(1, "Leve", 95, 115, false))
                ),
                List.of(new CondicaoSaudeRequest("ALERGIA", "Rinite", true)),
                List.of(new DispositivoExternoRequest(PlataformaRelogio.GARMIN, "token", LocalDateTime.now().plusDays(1)))
        );
    }
}
