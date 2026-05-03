package com.seucorre.usuario.application;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.usuario.application.dto.CondicaoSaudeRequest;
import com.seucorre.usuario.application.dto.DadosFisicosRequest;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.PerfilAtletaRequest;
import com.seucorre.usuario.application.dto.PerfilCorridaRequest;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.dto.UsuarioResponse;
import com.seucorre.usuario.application.dto.ZonaFCRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioAppServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder passwordEncoder;
    private UsuarioAppService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UsuarioAppService(repository, passwordEncoder);
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
