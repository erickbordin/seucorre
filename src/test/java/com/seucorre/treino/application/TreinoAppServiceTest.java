package com.seucorre.treino.application;

import com.seucorre.avaliacao.application.AvaliacaoAppService;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.application.dto.RegistroTreinoDTO;
import com.seucorre.treino.application.dto.RegistroTreinoRequest;
import com.seucorre.treino.application.dto.SessaoTreinoDTO;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.RegistroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TreinoAppServiceTest {

    private RegistroRepository registroRepository;
    private AvaliacaoAppService avaliacaoAppService;
    private TreinoAppService service;

    @BeforeEach
    void setUp() {
        registroRepository = mock(RegistroRepository.class);
        avaliacaoAppService = mock(AvaliacaoAppService.class);
        service = new TreinoAppService(registroRepository, avaliacaoAppService);
    }

    @Test
    void registrarExecucaoPersisteRegistroCalculaDesviosEAtualizaProgresso() {
        UUID treinoId = UUID.randomUUID();
        SessaoTreino sessaoTreino = criarSessao(treinoId);
        RegistroTreinoRequest request = new RegistroTreinoRequest(
                treinoId,
                StatusTreino.CONCLUIDO,
                new BigDecimal("8.80"),
                152,
                176,
                355,
                7,
                true,
                "panturrilha",
                false,
                false,
                "Treino executado com leve dor"
        );

        when(registroRepository.findSessaoTreinoById(treinoId)).thenReturn(Optional.of(sessaoTreino));
        when(registroRepository.save(any(RegistroTreino.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(avaliacaoAppService.atualizarProgressoSemanal(any(RegistroTreino.class))).thenReturn(new ProgressoSemanal());

        RegistroTreinoDTO dto = service.registrarExecucao(request);

        assertThat(dto.status()).isEqualTo(StatusTreino.CONCLUIDO);
        assertThat(dto.distanciaRealKm()).isEqualByComparingTo("8.80");
        assertThat(dto.alertaSaude()).isTrue();
        assertThat(dto.desvioDistanciaPercent()).isNotZero();
        assertThat(dto.desvioPace()).isNotZero();
        verify(registroRepository).save(any(RegistroTreino.class));
        verify(avaliacaoAppService).atualizarProgressoSemanal(any(RegistroTreino.class));
    }

    @Test
    void registrarExecucaoFalhaQuandoSessaoNaoExiste() {
        UUID treinoId = UUID.randomUUID();
        RegistroTreinoRequest request = new RegistroTreinoRequest(
                treinoId,
                StatusTreino.CONCLUIDO,
                new BigDecimal("5.00"),
                150,
                170,
                340,
                5,
                false,
                null,
                false,
                false,
                null
        );

        when(registroRepository.findSessaoTreinoById(treinoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarExecucao(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Sessão de treino não encontrada.");
    }

    @Test
    void listarHistoricoSessoesRetornaDtosOrdenadosPeloRepositorio() {
        UUID usuarioId = UUID.randomUUID();
        SessaoTreino sessaoUm = criarSessao(UUID.randomUUID());
        SessaoTreino sessaoDois = criarSessao(UUID.randomUUID());
        sessaoDois.setNumeroSemana(2);
        sessaoDois.setDataPrevista(LocalDate.of(2026, 5, 20));

        when(registroRepository.findSessoesByUsuarioIdOrderByDataPrevistaDesc(usuarioId))
                .thenReturn(List.of(sessaoDois, sessaoUm));

        List<SessaoTreinoDTO> historico = service.listarHistoricoSessoes(usuarioId);

        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).dataPrevista()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(historico.get(1).dataPrevista()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    private SessaoTreino criarSessao(UUID treinoId) {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setId(treinoId);
        sessaoTreino.setPlano(planoTreino);
        sessaoTreino.setNumeroSemana(1);
        sessaoTreino.setDataPrevista(LocalDate.of(2026, 5, 10));
        sessaoTreino.setTipo(TipoTreino.LONGO);
        sessaoTreino.setDistanciaKm(new BigDecimal("8.00"));
        sessaoTreino.setPaceAlvo(new BigDecimal("340.00"));
        sessaoTreino.setDescricao("Longo base");
        return sessaoTreino;
    }
}
