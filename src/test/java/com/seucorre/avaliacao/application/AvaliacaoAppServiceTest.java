package com.seucorre.avaliacao.application;

import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.avaliacao.infrastructure.ProgressoRepository;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.RegistroRepository;
import com.seucorre.usuario.domain.DadosFisicos;
import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvaliacaoAppServiceTest {

    private ProgressoRepository progressoRepository;
    private RegistroRepository registroRepository;
    private AvaliacaoAppService service;

    @BeforeEach
    void setUp() {
        progressoRepository = mock(ProgressoRepository.class);
        registroRepository = mock(RegistroRepository.class);
        service = new AvaliacaoAppService(progressoRepository, registroRepository);
    }

    @Test
    void atualizaProgressoSemanalComMetricasDerivadasDosRegistros() {
        RegistroTreino registroTreino = criarRegistro(StatusTreino.CONCLUIDO, "8.00", 340);
        RegistroTreino registroParcial = criarRegistro(StatusTreino.PARCIAL, "4.00", 360);
        RegistroTreino registroPerdido = criarRegistro(StatusTreino.PERDIDO, null, null);

        ProgressoSemanal progressoAnterior = new ProgressoSemanal();
        progressoAnterior.setVolumeKm(new BigDecimal("10.00"));

        when(progressoRepository.findByPlanoIdAndNumeroSemana(registroTreino.getSessaoTreino().getPlano().getId(), 1))
                .thenReturn(Optional.empty());
        when(registroRepository.findByPlanoIdAndNumeroSemana(registroTreino.getSessaoTreino().getPlano().getId(), 1))
                .thenReturn(List.of(registroTreino, registroParcial, registroPerdido));
        when(progressoRepository.save(org.mockito.ArgumentMatchers.any(ProgressoSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgressoSemanal atualizado = service.atualizarProgressoSemanal(registroTreino);

        assertThat(atualizado.getNumeroSemana()).isEqualTo(1);
        assertThat(atualizado.getVolumeKm()).isEqualByComparingTo("12.00");
        assertThat(atualizado.getPaceMedio()).isEqualByComparingTo("350.00");
        assertThat(atualizado.getTreinosConcluidos()).isEqualTo(1);
        assertThat(atualizado.getTreinosParciais()).isEqualTo(1);
        assertThat(atualizado.getTreinosPerdidos()).isEqualTo(1);
        assertThat(atualizado.getFcRepousoMedio()).isEqualTo(58);
        assertThat(atualizado.getDataInicioSemana()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    private RegistroTreino criarRegistro(StatusTreino statusTreino, String distanciaKm, Integer paceMedio) {
        Usuario usuario = new Usuario();
        DadosFisicos dadosFisicos = new DadosFisicos();
        dadosFisicos.setFcRepouso(58);
        usuario.setDadosFisicos(dadosFisicos);

        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());
        planoTreino.setUsuario(usuario);

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setPlano(planoTreino);
        sessaoTreino.setNumeroSemana(1);
        sessaoTreino.setDataPrevista(LocalDate.of(2026, 5, 10));

        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setSessaoTreino(sessaoTreino);
        registroTreino.setStatus(statusTreino);
        registroTreino.setDistanciaRealKm(distanciaKm == null ? null : new BigDecimal(distanciaKm));
        registroTreino.setPaceMedioReal(paceMedio);
        return registroTreino;
    }
}
