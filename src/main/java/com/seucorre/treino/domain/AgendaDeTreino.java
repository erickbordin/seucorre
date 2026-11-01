package com.seucorre.treino.domain;

import com.seucorre.shared.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AgendaDeTreino {

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

    private void agendar(PlanoTreino planoTreino, String diasSemanaTreino, LocalDate inicio) {
        List<DayOfWeek> diasTreino = lerDiasDeTreino(diasSemanaTreino);
        if (diasTreino.isEmpty()) {
            throw new BusinessRuleException("Dias preferenciais de treino são obrigatórios para agendar o plano.");
        }

        List<SessaoTreino> sessoes = planoTreino.getSessoes();
        if (sessoes == null || sessoes.isEmpty()) {
            return;
        }
        sessoes.sort(Comparator.comparing(SessaoTreino::getNumeroSemana, Comparator.nullsLast(Comparator.naturalOrder())));
        List<LocalDate> calendario = construirCalendario(sessoes.size(), diasTreino, inicio);
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
