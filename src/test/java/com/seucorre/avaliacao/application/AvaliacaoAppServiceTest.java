package com.seucorre.avaliacao.application;

import com.seucorre.avaliacao.application.dto.AnaliseRiscoDTO;
import com.seucorre.avaliacao.application.dto.CheckinSemanalRequest;
import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.avaliacao.infrastructure.CheckinRepository;
import com.seucorre.infra.events.EventPublisher;
import com.seucorre.infra.notification.NotificacaoService;
import com.seucorre.shared.domain.event.CheckinProcessadoEvent;
import com.seucorre.shared.domain.event.PlanoReescritoEvent;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.treino.domain.GeradorPlanoIA;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.RegistroTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.treino.infrastructure.PlanoRepository;
import com.seucorre.treino.infrastructure.RegistroRepository;
import com.seucorre.usuario.domain.DadosFisicos;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AvaliacaoAppServiceTest {

    private ProgressoAppService progressoAppService;
    private CheckinRepository checkinRepository;
    private PlanoRepository planoRepository;
    private UsuarioRepository usuarioRepository;
    private GeradorPlanoIA geradorPlanoIA;
    private EventPublisher eventPublisher;
    private RegistroRepository registroRepository;
    private NotificacaoService notificacaoService;
    private AvaliacaoAppService service;

    @BeforeEach
    void setUp() {
        progressoAppService = mock(ProgressoAppService.class);
        checkinRepository = mock(CheckinRepository.class);
        planoRepository = mock(PlanoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        geradorPlanoIA = mock(GeradorPlanoIA.class);
        eventPublisher = mock(EventPublisher.class);
        registroRepository = mock(RegistroRepository.class);
        notificacaoService = mock(NotificacaoService.class);
        service = new AvaliacaoAppService(
                progressoAppService,
                checkinRepository,
                planoRepository,
                usuarioRepository,
                geradorPlanoIA,
                eventPublisher,
                registroRepository,
                notificacaoService
        );
    }

    @Test
    void processaCheckinSemReescritaQuandoRiscoEhBaixo() {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario);
        CheckinSemanalRequest request = new CheckinSemanalRequest(
                usuario.getId(),
                planoTreino.getId(),
                2,
                LocalDate.of(2026, 5, 20),
                4,
                1,
                8,
                "Semana estavel"
        );

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(planoRepository.findById(planoTreino.getId())).thenReturn(Optional.of(planoTreino));
        when(checkinRepository.findByPlanoIdAndSemana(planoTreino.getId(), 2)).thenReturn(Optional.empty());
        when(progressoAppService.buscarProgressoSemana(planoTreino.getId(), 2)).thenReturn(null);
        when(progressoAppService.gerarAlertaSemanal(planoTreino.getId(), 2)).thenReturn(null);
        when(geradorPlanoIA.gerarAnaliseCheckin(org.mockito.ArgumentMatchers.any(CheckinSemanal.class)))
                .thenReturn("Risco baixo, manter plano.");
        when(checkinRepository.save(org.mockito.ArgumentMatchers.any(CheckinSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnaliseRiscoDTO dto = service.processarCheckin(request);

        assertThat(dto.nivelRisco()).isNotNull();
        assertThat(dto.precisaReescreverPlano()).isFalse();
        assertThat(dto.planoReescrito()).isFalse();
        assertThat(dto.analiseIA()).isEqualTo("Risco baixo, manter plano.");
        verify(geradorPlanoIA, never()).reescreverPlano(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList()
        );
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any(CheckinProcessadoEvent.class));
    }

    @Test
    void processaCheckinComReescritaQuandoRiscoExigeAjuste() {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario);
        PlanoTreino planoReescrito = criarPlano(usuario);
        planoReescrito.setId(null);

        ProgressoSemanal progressoSemanal = new ProgressoSemanal();
        progressoSemanal.setPlano(planoTreino);
        progressoSemanal.setNumeroSemana(3);
        progressoSemanal.setVolumeKm(new BigDecimal("28.00"));
        progressoSemanal.setTreinosConcluidos(2);
        progressoSemanal.setTreinosParciais(1);
        progressoSemanal.setTreinosPerdidos(0);

        CheckinSemanalRequest request = new CheckinSemanalRequest(
                usuario.getId(),
                planoTreino.getId(),
                3,
                LocalDate.of(2026, 5, 27),
                9,
                7,
                4,
                "Dor e fadiga altas"
        );

        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(planoRepository.findById(planoTreino.getId())).thenReturn(Optional.of(planoTreino));
        when(checkinRepository.findByPlanoIdAndSemana(planoTreino.getId(), 3)).thenReturn(Optional.empty());
        when(progressoAppService.buscarProgressoSemana(planoTreino.getId(), 3)).thenReturn(progressoSemanal);
        when(progressoAppService.listarUltimosProgressosDoPlano(planoTreino.getId())).thenReturn(List.of(progressoSemanal));
        when(progressoAppService.gerarAlertaSemanal(planoTreino.getId(), 3))
                .thenReturn("Alerta: a semana 3 ultrapassou a Regra dos 10%.");
        when(geradorPlanoIA.gerarAnaliseCheckin(org.mockito.ArgumentMatchers.any(CheckinSemanal.class)))
                .thenReturn("Risco alto, revisar a carga.");
        when(geradorPlanoIA.sugerirAjuste(progressoSemanal))
                .thenReturn("Reduzir volume em 20 por cento.");
        when(geradorPlanoIA.reescreverPlano(
                org.mockito.ArgumentMatchers.eq(planoTreino),
                org.mockito.ArgumentMatchers.any(CheckinSemanal.class),
                org.mockito.ArgumentMatchers.eq(List.of(progressoSemanal))
        ))
                .thenReturn(planoReescrito);
        when(planoRepository.save(org.mockito.ArgumentMatchers.any(PlanoTreino.class)))
                .thenAnswer(invocation -> {
                    PlanoTreino plano = invocation.getArgument(0);
                    if (plano.getId() == null) {
                        plano.setId(UUID.randomUUID());
                    }
                    return plano;
                });
        when(checkinRepository.save(org.mockito.ArgumentMatchers.any(CheckinSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnaliseRiscoDTO dto = service.processarCheckin(request);

        assertThat(dto.precisaReescreverPlano()).isTrue();
        assertThat(dto.planoReescrito()).isTrue();
        assertThat(dto.analiseIA()).contains("Risco alto, revisar a carga.");
        assertThat(dto.analiseIA()).contains("Ajuste semanal");
        assertThat(dto.analiseIA()).contains("Regra dos 10%");
        assertThat(planoTreino.getStatus()).isEqualTo(StatusPlano.CANCELADO);
        verify(geradorPlanoIA).reescreverPlano(
                org.mockito.ArgumentMatchers.eq(planoTreino),
                org.mockito.ArgumentMatchers.any(CheckinSemanal.class),
                org.mockito.ArgumentMatchers.eq(List.of(progressoSemanal))
        );
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any(PlanoReescritoEvent.class));
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any(CheckinProcessadoEvent.class));
    }

    @Test
    void listaHistoricosDeCheckinEProgressoEmDtos() {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario);

        CheckinSemanal checkinSemanal = new CheckinSemanal();
        checkinSemanal.setId(UUID.randomUUID());
        checkinSemanal.setUsuario(usuario);
        checkinSemanal.setPlano(planoTreino);
        checkinSemanal.setSemana(1);
        checkinSemanal.setDataCheckin(LocalDate.of(2026, 5, 4));
        checkinSemanal.setNivelEsforco(5);
        checkinSemanal.setNivelDor(2);
        checkinSemanal.setHorasSonoSemana(8);
        checkinSemanal.setAnaliseIA("Sem sinais de risco.");
        checkinSemanal.setPlanoReescrito(false);

        ProgressoSemanal progressoSemanal = new ProgressoSemanal();
        progressoSemanal.setId(UUID.randomUUID());
        progressoSemanal.setPlano(planoTreino);
        progressoSemanal.setNumeroSemana(1);
        progressoSemanal.setDataInicioSemana(LocalDate.of(2026, 5, 4));
        progressoSemanal.setTreinosConcluidos(2);
        progressoSemanal.setTreinosParciais(0);
        progressoSemanal.setTreinosPerdidos(1);

        when(checkinRepository.findByUsuarioIdOrderBySemanaAsc(usuario.getId())).thenReturn(List.of(checkinSemanal));
        when(progressoAppService.listarHistoricoProgresso(usuario.getId()))
                .thenReturn(List.of(ProgressoSemanalDTO.from(progressoSemanal)));

        List<AnaliseRiscoDTO> historicoCheckins = service.listarHistoricoCheckins(usuario.getId());
        List<ProgressoSemanalDTO> historicoProgresso = service.listarHistoricoProgresso(usuario.getId());

        assertThat(historicoCheckins).hasSize(1);
        assertThat(historicoCheckins.get(0).analiseIA()).isEqualTo("Sem sinais de risco.");
        assertThat(historicoProgresso).hasSize(1);
        assertThat(historicoProgresso.get(0).totalTreinos()).isEqualTo(3);
    }

    @Test
    void enviarLembretesMatinaisDisparaNotificacaoQuandoHaTreinoHojePendente() {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario);
        planoTreino.setStatus(StatusPlano.ATIVO);
        planoTreino.setDataInicio(LocalDate.now().minusDays(1));
        planoTreino.setTotalSemanas(4);

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setId(UUID.randomUUID());
        sessaoTreino.setPlano(planoTreino);
        sessaoTreino.setNumeroSemana(1);
        sessaoTreino.setDataPrevista(LocalDate.now());
        planoTreino.adicionarSessao(sessaoTreino);

        when(planoRepository.findByStatus(StatusPlano.ATIVO)).thenReturn(List.of(planoTreino));

        int lembretesEnviados = service.enviarLembretesMatinais();

        assertThat(lembretesEnviados).isEqualTo(1);
        verify(notificacaoService).enviarLembreteTreino(usuario.getId(), 1, 1);
    }

    @Test
    void abrirCheckinsSemanaisCriaCheckinQuandoNaoExisteParaSemanaAtual() {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario);
        planoTreino.setStatus(StatusPlano.ATIVO);
        planoTreino.setDataInicio(LocalDate.now().minusDays(1));
        planoTreino.setTotalSemanas(4);

        when(planoRepository.findByStatus(StatusPlano.ATIVO)).thenReturn(List.of(planoTreino));
        when(checkinRepository.findByPlanoIdAndSemana(planoTreino.getId(), 1)).thenReturn(Optional.empty());
        when(checkinRepository.save(org.mockito.ArgumentMatchers.any(CheckinSemanal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int checkinsAbertos = service.abrirCheckinsSemanais();

        assertThat(checkinsAbertos).isEqualTo(1);
        verify(checkinRepository).save(org.mockito.ArgumentMatchers.any(CheckinSemanal.class));
        verify(notificacaoService).enviarAberturaCheckin(usuario.getId(), 1);
    }

    @Test
    void recalcularProgressosAutomaticosAtualizaSemanaQuandoHaRegistros() {
        Usuario usuario = criarUsuario();
        PlanoTreino planoTreino = criarPlano(usuario);
        planoTreino.setStatus(StatusPlano.ATIVO);
        planoTreino.setDataInicio(LocalDate.now().minusDays(1));
        planoTreino.setTotalSemanas(4);

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setId(UUID.randomUUID());
        sessaoTreino.setPlano(planoTreino);
        sessaoTreino.setNumeroSemana(1);

        RegistroTreino registroTreino = new RegistroTreino();
        registroTreino.setId(UUID.randomUUID());
        registroTreino.setSessaoTreino(sessaoTreino);

        when(planoRepository.findByStatus(StatusPlano.ATIVO)).thenReturn(List.of(planoTreino));
        when(registroRepository.findByPlanoIdAndNumeroSemana(planoTreino.getId(), 1)).thenReturn(List.of(registroTreino));
        when(progressoAppService.atualizarProgressoSemanal(registroTreino)).thenReturn(new ProgressoSemanal());

        int progressosRecalculados = service.recalcularProgressosAutomaticos();

        assertThat(progressosRecalculados).isEqualTo(1);
        verify(progressoAppService).atualizarProgressoSemanal(registroTreino);
    }

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());

        DadosFisicos dadosFisicos = new DadosFisicos();
        dadosFisicos.setFcRepouso(58);
        usuario.setDadosFisicos(dadosFisicos);
        return usuario;
    }

    private PlanoTreino criarPlano(Usuario usuario) {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());
        planoTreino.setUsuario(usuario);
        return planoTreino;
    }
}
