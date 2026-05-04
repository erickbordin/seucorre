package com.seucorre.treino.application;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.application.dto.GerarPlanoRequest;
import com.seucorre.treino.application.dto.PlanoTreinoDTO;
import com.seucorre.treino.application.dto.SessaoTreinoDTO;
import com.seucorre.treino.domain.GeradorPlanoIA;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.PerfilCorrida;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import com.seucorre.treino.infrastructure.PlanoRepository;
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

class PlanoAppServiceTest {

    private PlanoRepository planoRepository;
    private UsuarioRepository usuarioRepository;
    private GeradorPlanoIA geradorPlanoIA;
    private PlanoAppService service;

    @BeforeEach
    void setUp() {
        planoRepository = mock(PlanoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        geradorPlanoIA = mock(GeradorPlanoIA.class);
        service = new PlanoAppService(planoRepository, usuarioRepository, geradorPlanoIA);
    }

    @Test
    void criarPlanoCarregaPerfilCompletoGeraESalvaPlano() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuario(usuarioId);
        PerfilCorrida perfilCorrida = new PerfilCorrida();
        PlanoTreino planoTreino = criarPlano(usuario);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findPerfilCorridaByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilCorrida));
        when(geradorPlanoIA.gerarPlano(usuario, Objetivo.COMPLETAR_10K)).thenReturn(planoTreino);
        when(planoRepository.save(planoTreino)).thenReturn(planoTreino);

        PlanoTreinoDTO dto = service.criarPlano(usuarioId, new GerarPlanoRequest(Objetivo.COMPLETAR_10K));

        assertThat(usuario.getPerfilAtleta().getPerfilCorrida()).isSameAs(perfilCorrida);
        assertThat(dto.id()).isEqualTo(planoTreino.getId());
        assertThat(dto.sessoes()).hasSize(1);
        verify(geradorPlanoIA).gerarPlano(usuario, Objetivo.COMPLETAR_10K);
        verify(planoRepository).save(planoTreino);
    }

    @Test
    void criarPlanoFalhaQuandoUsuarioNaoExiste() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarPlano(usuarioId, new GerarPlanoRequest(Objetivo.COMPLETAR_10K)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Usuário não encontrado.");
    }

    @Test
    void pausarPlanoAtualizaStatus() {
        UUID planoId = UUID.randomUUID();
        PlanoTreino planoTreino = criarPlano(criarUsuario(UUID.randomUUID()));
        planoTreino.setId(planoId);

        when(planoRepository.findById(planoId)).thenReturn(Optional.of(planoTreino));
        when(planoRepository.save(any(PlanoTreino.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanoTreinoDTO dto = service.pausarPlano(planoId);

        assertThat(dto.status()).isEqualTo(StatusPlano.PAUSADO);
    }

    @Test
    void reativarPlanoAtualizaStatus() {
        UUID planoId = UUID.randomUUID();
        PlanoTreino planoTreino = criarPlano(criarUsuario(UUID.randomUUID()));
        planoTreino.setId(planoId);
        planoTreino.pausar();

        when(planoRepository.findById(planoId)).thenReturn(Optional.of(planoTreino));
        when(planoRepository.save(any(PlanoTreino.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanoTreinoDTO dto = service.reativarPlano(planoId);

        assertThat(dto.status()).isEqualTo(StatusPlano.ATIVO);
    }

    @Test
    void listaSessoesDaSemanaComoDto() {
        UUID usuarioId = UUID.randomUUID();
        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setId(UUID.randomUUID());
        sessaoTreino.setNumeroSemana(2);
        sessaoTreino.setDataPrevista(LocalDate.of(2026, 5, 12));
        sessaoTreino.setTipo(TipoTreino.LONGO);
        sessaoTreino.setDistanciaKm(new BigDecimal("12.00"));

        when(planoRepository.findSessoesByUsuarioIdAndNumeroSemana(usuarioId, 2))
                .thenReturn(List.of(sessaoTreino));

        List<SessaoTreinoDTO> sessoes = service.listarSessoesDaSemana(usuarioId, 2);

        assertThat(sessoes).hasSize(1);
        assertThat(sessoes.get(0).numeroSemana()).isEqualTo(2);
        assertThat(sessoes.get(0).tipo()).isEqualTo(TipoTreino.LONGO);
    }

    private Usuario criarUsuario(UUID usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Ana Runner");
        usuario.setEmail("ana@email.com");

        PerfilAtleta perfilAtleta = new PerfilAtleta();
        perfilAtleta.setNivelCondicionamento(NivelCondicionamento.INTERMEDIARIO);
        perfilAtleta.setObjetivo(Objetivo.COMPLETAR_10K);
        perfilAtleta.setJaCorre(true);
        perfilAtleta.setDiasDisponiveisSemana(4);
        perfilAtleta.setDiasSemanaTreino("SEG,QUA,SEX,SAB");
        usuario.setPerfilAtleta(perfilAtleta);
        return usuario;
    }

    private PlanoTreino criarPlano(Usuario usuario) {
        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setId(UUID.randomUUID());
        planoTreino.setUsuario(usuario);
        planoTreino.setTotalSemanas(2);
        planoTreino.setDataInicio(LocalDate.of(2026, 5, 5));
        planoTreino.setDataFim(LocalDate.of(2026, 5, 18));
        planoTreino.setKmTotais(new BigDecimal("18.00"));
        planoTreino.setResumoIA("Plano base");

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setNumeroSemana(1);
        sessaoTreino.setDataPrevista(LocalDate.of(2026, 5, 5));
        sessaoTreino.setTipo(TipoTreino.REGENERATIVO);
        sessaoTreino.setDistanciaKm(new BigDecimal("6.00"));
        planoTreino.adicionarSessao(sessaoTreino);

        return planoTreino;
    }
}
