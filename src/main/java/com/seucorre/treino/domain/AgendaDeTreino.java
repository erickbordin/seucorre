package com.seucorre.treino.domain;

import com.seucorre.shared.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AgendaDeTreino {

    private static final int MAXIMO_SEMANAS_DE_BASE = 4;
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Map<String, DayOfWeek> DIAS_POR_CODIGO = Map.of(
            "SEG", DayOfWeek.MONDAY,
            "TER", DayOfWeek.TUESDAY,
            "QUA", DayOfWeek.WEDNESDAY,
            "QUI", DayOfWeek.THURSDAY,
            "SEX", DayOfWeek.FRIDAY,
            "SAB", DayOfWeek.SATURDAY,
            "DOM", DayOfWeek.SUNDAY
    );

    private final Clock clock;

    public AgendaDeTreino(Clock clock) {
        this.clock = clock;
    }

    public static List<DayOfWeek> lerDiasDeTreino(String diasSemanaTreino) {
        if (diasSemanaTreino == null || diasSemanaTreino.isBlank()) {
            return List.of();
        }

        Set<DayOfWeek> dias = new LinkedHashSet<>();
        for (String parte : diasSemanaTreino.split(",")) {
            DayOfWeek dia = DIAS_POR_CODIGO.get(parte.trim().toUpperCase(Locale.ROOT));
            if (dia != null) {
                dias.add(dia);
            }
        }
        return dias.stream().sorted().toList();
    }

    public void agendar(PlanoTreino planoTreino, String diasSemanaTreino) {
        agendar(planoTreino, diasSemanaTreino, LocalDate.now(clock));
    }

    public void agendarDepoisDe(PlanoTreino planoTreino, String diasSemanaTreino, LocalDate ultimaDataOcupada) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = ultimaDataOcupada == null || ultimaDataOcupada.isBefore(hoje) ? hoje : ultimaDataOcupada.plusDays(1);
        agendar(planoTreino, diasSemanaTreino, inicio);
    }

    public void agendarAteAProva(PlanoTreino planoTreino, String diasSemanaTreino, LocalDate dataProva) {
        List<DayOfWeek> diasTreino = lerDiasObrigatorios(diasSemanaTreino);
        LocalDate hoje = LocalDate.now(clock);
        if (!dataProva.isAfter(hoje)) {
            throw new BusinessRuleException("A data da prova precisa ser depois de hoje.");
        }
        int sessoesAntesDaProva = planoTreino.getSessoes().size() - 1;
        List<LocalDate> diasLivres = diasDeTreinoAntesDe(dataProva, diasTreino, hoje);
        if (diasLivres.size() < sessoesAntesDaProva) {
            LocalDate primeiraDataPossivel = construirCalendario(sessoesAntesDaProva, diasTreino, hoje).get(sessoesAntesDaProva - 1).plusDays(1);
            throw new BusinessRuleException("O plano tem %d semanas e não cabe até %s. Escolha uma prova a partir de %s ou gere o plano sem data."
                    .formatted(planoTreino.getTotalSemanas(), dataProva.format(DATA_BR), primeiraDataPossivel.format(DATA_BR)));
        }
        int treinosPorSemana = planoTreino.obterSessoesDaSemana(1).size();
        planoTreino.adicionarSemanasDeBase(Math.min((diasLivres.size() - sessoesAntesDaProva) / treinosPorSemana, MAXIMO_SEMANAS_DE_BASE));
        List<SessaoTreino> sessoes = ordenar(planoTreino);
        List<LocalDate> datas = new ArrayList<>(diasLivres.subList(diasLivres.size() - (sessoes.size() - 1), diasLivres.size()));
        datas.add(dataProva);
        aplicarDatas(planoTreino, sessoes, datas);
    }

    private void agendar(PlanoTreino planoTreino, String diasSemanaTreino, LocalDate inicio) {
        List<DayOfWeek> diasTreino = lerDiasObrigatorios(diasSemanaTreino);
        List<SessaoTreino> sessoes = ordenar(planoTreino);
        if (sessoes.isEmpty()) {
            return;
        }
        aplicarDatas(planoTreino, sessoes, construirCalendario(sessoes.size(), diasTreino, inicio));
    }

    private List<DayOfWeek> lerDiasObrigatorios(String diasSemanaTreino) {
        List<DayOfWeek> diasTreino = lerDiasDeTreino(diasSemanaTreino);
        if (diasTreino.isEmpty()) {
            throw new BusinessRuleException("Dias preferenciais de treino são obrigatórios para agendar o plano.");
        }
        return diasTreino;
    }

    private List<SessaoTreino> ordenar(PlanoTreino planoTreino) {
        List<SessaoTreino> sessoes = planoTreino.getSessoes();
        if (sessoes == null) {
            return List.of();
        }
        sessoes.sort(Comparator.comparing(SessaoTreino::getNumeroSemana, Comparator.nullsLast(Comparator.naturalOrder())));
        return sessoes;
    }

    private void aplicarDatas(PlanoTreino planoTreino, List<SessaoTreino> sessoes, List<LocalDate> calendario) {
        for (int indice = 0; indice < sessoes.size(); indice++) {
            sessoes.get(indice).setDataPrevista(calendario.get(indice));
        }
        planoTreino.setDataInicio(calendario.get(0));
        planoTreino.setDataFim(calendario.get(calendario.size() - 1));
        if (planoTreino.getTotalSemanas() == null) {
            planoTreino.setTotalSemanas(sessoes.stream()
                    .map(SessaoTreino::getNumeroSemana)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(null));
        }
        planoTreino.setKmTotais(sessoes.stream()
                .map(SessaoTreino::getDistanciaKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private List<LocalDate> diasDeTreinoAntesDe(LocalDate limite, List<DayOfWeek> diasTreino, LocalDate inicio) {
        List<LocalDate> datas = new ArrayList<>();
        for (LocalDate cursor = inicio; cursor.isBefore(limite); cursor = cursor.plusDays(1)) {
            if (diasTreino.contains(cursor.getDayOfWeek())) {
                datas.add(cursor);
            }
        }
        return datas;
    }

    private List<LocalDate> construirCalendario(int totalSessoes, List<DayOfWeek> diasTreino, LocalDate inicio) {
        List<LocalDate> datas = new ArrayList<>(totalSessoes);
        LocalDate cursor = inicio;
        while (datas.size() < totalSessoes) {
            if (diasTreino.contains(cursor.getDayOfWeek())) {
                datas.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return datas;
    }
}
