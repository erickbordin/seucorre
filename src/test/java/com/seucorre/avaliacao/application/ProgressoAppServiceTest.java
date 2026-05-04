package com.seucorre.avaliacao.application;

import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProgressoAppServiceTest {

    private ProgressoRepository progressoRepository;
    private RegistroRepository registroRepository;
    private ProgressoAppService service;

    @BeforeEach
    void setUp() {
        progressoRepository = mock(ProgressoRepository.class);
        registroRepository = mock(RegistroRepository.class);
        service = new ProgressoAppService(progressoRepository, registroRepository);
    }

    @Test
    void atualizaProgressoSemanalComMetricasDerivadasDosRegistros() {
        RegistroTreino registroTreino = criarRegistro(StatusTreino.CONCLUIDO, "8.00", 340, 1);
        RegistroTreino registroParcial = criarRegistro(StatusTreino.PARCIAL, "4.00", 360, 1);
        RegistroTreino registroPerdido = criarRegistro(StatusTreino.PERDIDO, null, null, 1);

        when(progressoRepository.findByPlanoIdAndNumeroSemana(registroTreino.getSessaoTreino().getPlano().getId(), 1))
                .thenReturn(Optional.empty());
        when(registroRepository.findByPlanoIdAndNumeroSemana(registroTreino.getSessaoTreino().getPlano().getId(), 1))
                .thenReturn(List.of(registroTreino, registroParcial, registroPerdido));
        when(progressoRepository.save(any(ProgressoSemanal.class)))
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

    @Test
    void geraAlertaQuandoUltrapassaRegraDosDezPorcento() {
        UUID planoId = UUID.randomUUID();

        ProgressoSemanal anterior = new ProgressoSemanal();
        anterior.setPlano(criarPlano(criarUsuario(), planoId));
        anterior.setNumeroSemana(1);
        anterior.setVolumeKm(new BigDecimal("20.00"));

        ProgressoSemanal atual = new ProgressoSemanal();
        atual.setPlano(criarPlano(criarUsuario(), planoId));
        atual.setNumeroSemana(2);
        atual.setVolumeKm(new BigDecimal("24.00"));

        when(progressoRepository.findByPlanoIdAndNumeroSemana(planoId, 2)).thenReturn(Optional.of(atual));
        when(progressoRepository.findByPlanoIdAndNumeroSemana(planoId, 1)).thenReturn(Optional.of(anterior));

        String alerta = service.gerarAlertaSemanal(planoId, 2);

        assertThat(alerta).contains("Regra dos 10%");
        assertThat(alerta).contains("20.00%");
    }

    @Test
    void listaHistoricoProgressoEmDtos() {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario, UUID.randomUUID());

        ProgressoSemanal progressoSemanal = new ProgressoSemanal();
        progressoSemanal.setId(UUID.randomUUID());
        progressoSemanal.setPlano(planoTreino);
        progressoSemanal.setNumeroSemana(3);
        progressoSemanal.setTreinosConcluidos(2);
        progressoSemanal.setTreinosParciais(1);
        progressoSemanal.setTreinosPerdidos(1);

        when(progressoRepository.findByPlanoUsuarioIdOrderByNumeroSemanaAsc(usuario.getId()))
                .thenReturn(List.of(progressoSemanal));

        List<ProgressoSemanalDTO> historico = service.listarHistoricoProgresso(usuario.getId());

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).numeroSemana()).isEqualTo(3);
        assertThat(historico.get(0).totalTreinos()).isEqualTo(4);
    }

    private RegistroTreino criarRegistro(StatusTreino statusTreino, String distanciaKm, Integer paceMedio, Integer semana) {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario, UUID.randomUUID());

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setPlano(planoTreino);
        sessaoTreino.setNumeroSemana(semana);
        sessaoTreino.setDataPrevista(LocalDate.of(2026, 5, 10));

        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setSessaoTreino(sessaoTreino);
        registroTreino.setStatus(statusTreino);
        registroTreino.setDistanciaRealKm(distanciaKm == null ? null : new BigDecimal(distanciaKm));
        registroTreino.setPaceMedioReal(paceMedio);
        return registroTreino;
    }

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());

        DadosFisicos dadosFisicos = new DadosFisicos();
        dadosFisicos.setFcRepouso(58);
        usuario.setDadosFisicos(dadosFisicos);
        return usuario;
    }

    private PlanoTreino criarPlano(Usuario usuario, UUID planoId) {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(planoId);
        planoTreino.setUsuario(usuario);
        return planoTreino;
    }
}
