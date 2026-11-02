package com.seucorre.treino.application;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.treino.domain.AgendaDeTreino;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.PlanoTreinoParser;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
public class PlanoPresetService {

    private static final String BASE_RESOURCE_PATH = "classpath:planos-presets/";
    private static final EnumSet<Objetivo> SUPPORTED_OBJECTIVES = EnumSet.of(
            Objetivo.SAUDE_GERAL,
            Objetivo.COMPLETAR_5K,
            Objetivo.COMPLETAR_10K,
            Objetivo.COMPLETAR_MEIA_MARATONA
    );
    private static final EnumSet<Objetivo> OBJETIVOS_COM_PROVA = EnumSet.of(
            Objetivo.COMPLETAR_5K,
            Objetivo.COMPLETAR_10K,
            Objetivo.COMPLETAR_MEIA_MARATONA
    );
    private static final EnumSet<NivelCondicionamento> SUPPORTED_LEVELS = EnumSet.of(
            NivelCondicionamento.INICIANTE,
            NivelCondicionamento.INTERMEDIARIO,
            NivelCondicionamento.AVANCADO
    );

    private final ResourceLoader resourceLoader;
    private final AgendaDeTreino agendaDeTreino;
    private final PlanoTreinoParser planoTreinoParser;

    @Autowired
    public PlanoPresetService(ResourceLoader resourceLoader, Clock clock) {
        this(resourceLoader, clock, new PlanoTreinoParser());
    }

    PlanoPresetService(ResourceLoader resourceLoader, Clock clock, PlanoTreinoParser planoTreinoParser) {
        this.resourceLoader = resourceLoader;
        this.agendaDeTreino = new AgendaDeTreino(clock);
        this.planoTreinoParser = planoTreinoParser;
    }

    public PlanoTreino gerarPlano(Usuario usuario, Objetivo objetivo) {
        validarUsuario(usuario);

        Objetivo objetivoEfetivo = objetivo != null ? objetivo : usuario.getObjetivo();
        PerfilAtleta perfilAtleta = usuario.getPerfilAtleta();
        NivelCondicionamento nivel = perfilAtleta == null ? null : perfilAtleta.getNivelCondicionamento();
        Integer diasDisponiveis = usuario.getDiasDisponiveisSemana();
        List<DayOfWeek> diasTreino = AgendaDeTreino.lerDiasDeTreino(usuario.getDiasSemanaTreino());

        validarCombinacaoSuportada(objetivoEfetivo, nivel, diasDisponiveis, diasTreino);

        String resourcePath = construirResourcePath(objetivoEfetivo, nivel, diasDisponiveis);
        PlanoTreino planoTreino = planoTreinoParser.parsear(lerResource(resourcePath), usuario);
        LocalDate dataProva = perfilAtleta.getDataProva();
        if (dataProva != null && objetivoEfetivo == perfilAtleta.getObjetivo() && OBJETIVOS_COM_PROVA.contains(objetivoEfetivo)) {
            planoTreino.setDataProva(dataProva);
            planoTreino.setNomeProva(perfilAtleta.getNomeProva());
            agendaDeTreino.agendarAteAProva(planoTreino, usuario.getDiasSemanaTreino(), dataProva);
        } else {
            agendaDeTreino.agendar(planoTreino, usuario.getDiasSemanaTreino());
        }
        return planoTreino;
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário é obrigatório.");
        }
        if (!usuario.estaAptoParaTreinar()) {
            throw new BusinessRuleException("Usuário não está apto para gerar plano de treino.");
        }
    }

    private void validarCombinacaoSuportada(Objetivo objetivo,
                                            NivelCondicionamento nivel,
                                            Integer diasDisponiveis,
                                            List<DayOfWeek> diasTreino) {
        if (objetivo == null) {
            throw new BusinessRuleException("Objetivo é obrigatório para gerar plano preset.");
        }
        if (nivel == null) {
            throw new BusinessRuleException("Nível de condicionamento é obrigatório para gerar plano preset.");
        }
        if (diasDisponiveis == null || diasDisponiveis <= 0) {
            throw new BusinessRuleException("Dias disponíveis por semana são obrigatórios para gerar plano preset.");
        }
        if (diasTreino.isEmpty()) {
            throw new BusinessRuleException("Dias preferenciais de treino são obrigatórios para gerar plano preset.");
        }
        if (!SUPPORTED_OBJECTIVES.contains(objetivo)) {
            throw new BusinessRuleException("O objetivo " + objetivo.getDescricao() + " ainda não tem plano disponível no SeuCorre.");
        }
        if (!SUPPORTED_LEVELS.contains(nivel)) {
            throw new BusinessRuleException("O nível " + nivel.getDescricao() + " ainda não tem plano disponível no SeuCorre.");
        }
        if (diasDisponiveis < 3 || diasDisponiveis > 4) {
            throw new BusinessRuleException("Os planos do SeuCorre estão disponíveis para 3 ou 4 dias de treino por semana.");
        }
        if (diasTreino.size() < diasDisponiveis) {
            throw new BusinessRuleException("Os dias preferenciais de treino não cobrem a disponibilidade semanal informada.");
        }
    }

    private String construirResourcePath(Objetivo objetivo, NivelCondicionamento nivel, Integer diasDisponiveis) {
        return BASE_RESOURCE_PATH
                + normalizarObjetivo(objetivo)
                + "/"
                + normalizarNivel(nivel)
                + "/"
                + diasDisponiveis
                + "-dias.json";
    }

    private String lerResource(String resourcePath) {
        Resource resource = resourceLoader.getResource(resourcePath);
        if (!resource.exists()) {
            throw new BusinessRuleException("Não existe plano preset cadastrado para a combinação solicitada.");
        }

        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler o plano preset em " + resourcePath + ".", exception);
        }
    }

    private String normalizarObjetivo(Objetivo objetivo) {
        return objetivo.name().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizarNivel(NivelCondicionamento nivel) {
        NivelCondicionamento nivelDoCatalogo = nivel == NivelCondicionamento.AVANCADO ? NivelCondicionamento.INTERMEDIARIO : nivel;
        return nivelDoCatalogo.name().toLowerCase(java.util.Locale.ROOT);
    }
}
