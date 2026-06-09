package com.seucorre.treino.application;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.domain.PlanoTreinoParser;
import com.seucorre.treino.domain.SessaoTreino;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PlanoPresetService {

    private static final String BASE_RESOURCE_PATH = "classpath:planos-presets/";
    private static final Map<String, DayOfWeek> DAYS_BY_CODE = Map.of(
            "SEG", DayOfWeek.MONDAY,
            "TER", DayOfWeek.TUESDAY,
            "QUA", DayOfWeek.WEDNESDAY,
            "QUI", DayOfWeek.THURSDAY,
            "SEX", DayOfWeek.FRIDAY,
            "SAB", DayOfWeek.SATURDAY,
            "DOM", DayOfWeek.SUNDAY
    );
    private static final EnumSet<Objetivo> SUPPORTED_OBJECTIVES = EnumSet.of(
            Objetivo.SAUDE_GERAL,
            Objetivo.COMPLETAR_5K,
            Objetivo.COMPLETAR_10K
    );
    private static final EnumSet<NivelCondicionamento> SUPPORTED_LEVELS = EnumSet.of(
            NivelCondicionamento.INICIANTE,
            NivelCondicionamento.INTERMEDIARIO
    );

    private final ResourceLoader resourceLoader;
    private final Clock clock;
    private final PlanoTreinoParser planoTreinoParser;

    @Autowired
    public PlanoPresetService(ResourceLoader resourceLoader, Clock clock) {
        this(resourceLoader, clock, new PlanoTreinoParser());
    }

    PlanoPresetService(ResourceLoader resourceLoader, Clock clock, PlanoTreinoParser planoTreinoParser) {
        this.resourceLoader = resourceLoader;
        this.clock = clock;
        this.planoTreinoParser = planoTreinoParser;
    }

    public PlanoTreino gerarPlano(Usuario usuario, Objetivo objetivo) {
        validarUsuario(usuario);

        Objetivo objetivoEfetivo = objetivo != null ? objetivo : usuario.getObjetivo();
        PerfilAtleta perfilAtleta = usuario.getPerfilAtleta();
        NivelCondicionamento nivel = perfilAtleta == null ? null : perfilAtleta.getNivelCondicionamento();
        Integer diasDisponiveis = usuario.getDiasDisponiveisSemana();
        List<DayOfWeek> diasTreino = parseDiasTreino(usuario.getDiasSemanaTreino());

        validarCombinacaoSuportada(objetivoEfetivo, nivel, diasDisponiveis, diasTreino);

        String resourcePath = construirResourcePath(objetivoEfetivo, nivel, diasDisponiveis);
        PlanoTreino planoTreino = planoTreinoParser.parsear(lerResource(resourcePath), usuario);
        distribuirDatas(planoTreino, diasTreino);
        recalcularMetadados(planoTreino);
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
            throw new BusinessRuleException("O objetivo " + objetivo.getDescricao() + " ainda não possui plano preset no MVP.");
        }
        if (!SUPPORTED_LEVELS.contains(nivel)) {
            throw new BusinessRuleException("O nível " + nivel.getDescricao() + " ainda não possui plano preset no MVP.");
        }
        if (diasDisponiveis < 3 || diasDisponiveis > 4) {
            throw new BusinessRuleException("Os planos preset do MVP estão disponíveis apenas para 3 ou 4 dias por semana.");
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

    private List<DayOfWeek> parseDiasTreino(String diasSemanaTreino) {
        if (diasSemanaTreino == null || diasSemanaTreino.isBlank()) {
            return List.of();
        }

        Map<DayOfWeek, DayOfWeek> unicos = new LinkedHashMap<>();
        for (String parte : diasSemanaTreino.split(",")) {
            String codigo = parte == null ? "" : parte.trim().toUpperCase(Locale.ROOT);
            DayOfWeek dayOfWeek = DAYS_BY_CODE.get(codigo);
            if (dayOfWeek != null) {
                unicos.put(dayOfWeek, dayOfWeek);
            }
        }

        return unicos.values().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private void distribuirDatas(PlanoTreino planoTreino, List<DayOfWeek> diasTreino) {
        if (planoTreino == null || planoTreino.getSessoes() == null || planoTreino.getSessoes().isEmpty()) {
            return;
        }

        List<LocalDate> calendario = construirCalendario(planoTreino.getSessoes().size(), diasTreino);
        for (int index = 0; index < planoTreino.getSessoes().size(); index++) {
            planoTreino.getSessoes().get(index).setDataPrevista(calendario.get(index));
        }
    }

    private List<LocalDate> construirCalendario(int totalSessoes, List<DayOfWeek> diasTreino) {
        List<LocalDate> datas = new ArrayList<>(totalSessoes);
        LocalDate cursor = LocalDate.now(clock);

        while (!diasTreino.contains(cursor.getDayOfWeek())) {
            cursor = cursor.plusDays(1);
        }

        while (datas.size() < totalSessoes) {
            if (diasTreino.contains(cursor.getDayOfWeek())) {
                datas.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }

        return datas;
    }

    private void recalcularMetadados(PlanoTreino planoTreino) {
        List<SessaoTreino> sessoes = planoTreino.getSessoes();
        if (sessoes == null || sessoes.isEmpty()) {
            return;
        }

        sessoes.sort(Comparator
                .comparing(SessaoTreino::getNumeroSemana, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SessaoTreino::getDataPrevista, Comparator.nullsLast(Comparator.naturalOrder())));

        planoTreino.setDataInicio(sessoes.stream()
                .map(SessaoTreino::getDataPrevista)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null));

        planoTreino.setDataFim(sessoes.stream()
                .map(SessaoTreino::getDataPrevista)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null));

        if (planoTreino.getTotalSemanas() == null) {
            planoTreino.setTotalSemanas(sessoes.stream()
                    .map(SessaoTreino::getNumeroSemana)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(null));
        }

        BigDecimal kmTotais = sessoes.stream()
                .map(SessaoTreino::getDistanciaKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        planoTreino.setKmTotais(kmTotais);
    }

    private String normalizarObjetivo(Objetivo objetivo) {
        return objetivo.name().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizarNivel(NivelCondicionamento nivel) {
        return nivel.name().toLowerCase(java.util.Locale.ROOT);
    }
}
