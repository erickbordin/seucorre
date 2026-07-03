package com.seucorre.avaliacao.application;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.enums.NivelRisco;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ManualCheckinAnalysisService {

    public String gerarAnalise(CheckinSemanal checkinSemanal,
                               ProgressoSemanal progressoSemanal,
                               String alertaSemanal) {
        if (checkinSemanal == null) {
            throw new IllegalArgumentException("Check-in e obrigatorio.");
        }

        List<String> linhas = new ArrayList<>();
        linhas.add("Risco atual: " + descreverRisco(checkinSemanal.avaliarNivelRisco()) + ".");
        linhas.add(descreverRecuperacao(checkinSemanal));

        if (progressoSemanal != null) {
            linhas.add("Resumo da semana: " + progressoSemanal.gerarResumo() + ".");
        }
        if (alertaSemanal != null && !alertaSemanal.isBlank()) {
            linhas.add(alertaSemanal.trim());
        }

        if (checkinSemanal.precisaReescreverPlano()) {
            linhas.add("Acao manual recomendada: revisar volume, intensidade e distribuicao da proxima semana sem reescrita automatica.");
        } else {
            linhas.add("Acao manual recomendada: manter o plano atual e seguir monitorando os sinais do atleta.");
        }

        return String.join("\n", linhas);
    }

    private String descreverRisco(NivelRisco nivelRisco) {
        if (nivelRisco == null) {
            return "nao classificado";
        }

        return switch (nivelRisco) {
            case BAIXO -> "baixo";
            case MODERADO -> "moderado";
            case ALTO -> "alto";
            case CRITICO -> "critico";
        };
    }

    private String descreverRecuperacao(CheckinSemanal checkinSemanal) {
        if (checkinSemanal.indicaSobrecarga() && checkinSemanal.indicaRecuperacaoInsuficiente()) {
            return "Ha sinais combinados de sobrecarga e recuperacao insuficiente.";
        }
        if (checkinSemanal.indicaSobrecarga()) {
            return "Ha sinais de sobrecarga na semana atual.";
        }
        return checkinSemanal.indicaRecuperacaoInsuficiente()
                ? "Ha sinais de recuperacao insuficiente na semana atual."
                : "Os sinais atuais indicam boa tolerancia a carga planejada.";
    }
}
