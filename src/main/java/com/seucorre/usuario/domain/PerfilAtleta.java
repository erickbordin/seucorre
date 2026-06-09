package com.seucorre.usuario.domain;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Embeddable
@Data
public class PerfilAtleta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_condicionamento")
    private NivelCondicionamento nivelCondicionamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "objetivo")
    private Objetivo objetivo;

    @Column(name = "ja_corre")
    private Boolean jaCorre;

    @Column(name = "dias_disponiveis_semana")
    private Integer diasDisponiveisSemana;

    @Column(name = "dias_semana_treino", length = 100)
    private String diasSemanaTreino;

    @Transient
    private PerfilCorrida perfilCorrida;

    public String gerarResumoParaIA() {
        return construirResumoBase() + ".";
    }

    public String gerarResumoParaIA(DadosFisicos dadosFisicos,
                                    PerfilCorrida perfilCorrida,
                                    List<CondicaoSaude> condicoesSaude) {
        List<String> partes = new ArrayList<>();
        partes.add(construirResumoBase());

        if (dadosFisicos != null) {
            partes.add(dadosFisicos.gerarResumoMetricasParaIA());
        }

        if (perfilCorrida != null) {
            partes.add("VO2 estimado: " + valorOuNaoInformado(perfilCorrida.getVo2Estimado()));
            partes.add("Zonas de FC cadastradas: " + resumirZonasFc(perfilCorrida));
        }

        partes.add("Condições de saúde ativas: " + resumirCondicoesAtivas(condicoesSaude));

        return partes.stream()
                .filter(Objects::nonNull)
                .filter(parte -> !parte.isBlank())
                .collect(Collectors.joining(". ")) + ".";
    }

    public boolean podeCorrer() {
        return nivelCondicionamento != null && objetivo != null && diasDisponiveisSemana != null && diasDisponiveisSemana > 0;
    }

    public void atualizarPerfilCorrida(PerfilCorrida perfilCorrida) {
        this.perfilCorrida = perfilCorrida;
    }

    private String construirResumoBase() {
        List<String> partes = new ArrayList<>();
        partes.add("Nível de condicionamento: " + descreverNivel());
        partes.add("Objetivo principal: " + descreverObjetivo());
        partes.add("Já corre atualmente: " + descreverExperiencia());
        partes.add("Disponibilidade semanal: " + descreverDisponibilidade());
        partes.add("Dias preferenciais para treino: " + descreverDiasTreino());
        return String.join(". ", partes);
    }

    private String descreverNivel() {
        return nivelCondicionamento == null ? "não informado" : nivelCondicionamento.getDescricao();
    }

    private String descreverObjetivo() {
        return objetivo == null ? "não informado" : objetivo.getDescricao();
    }

    private String descreverExperiencia() {
        if (jaCorre == null) {
            return "não informado";
        }
        return Boolean.TRUE.equals(jaCorre) ? "sim" : "não";
    }

    private String descreverDisponibilidade() {
        if (diasDisponiveisSemana == null) {
            return "não informada";
        }
        if (diasDisponiveisSemana == 1) {
            return "1 dia por semana";
        }
        return diasDisponiveisSemana + " dias por semana";
    }

    private String descreverDiasTreino() {
        if (diasSemanaTreino == null || diasSemanaTreino.isBlank()) {
            return "não informados";
        }
        return diasSemanaTreino.trim();
    }

    private String resumirZonasFc(PerfilCorrida perfilCorrida) {
        if (perfilCorrida.getZonasFc() == null || perfilCorrida.getZonasFc().isEmpty()) {
            return "não cadastradas";
        }
        return perfilCorrida.getZonasFc().stream()
                .map(zona -> zona.getNome() != null && !zona.getNome().isBlank()
                        ? zona.getNome()
                        : "Zona " + zona.getZona())
                .collect(Collectors.joining(", "));
    }

    private String resumirCondicoesAtivas(List<CondicaoSaude> condicoesSaude) {
        if (condicoesSaude == null || condicoesSaude.isEmpty()) {
            return "nenhuma";
        }

        List<String> condicoesAtivas = condicoesSaude.stream()
                .filter(condicao -> Boolean.TRUE.equals(condicao.getAtiva()))
                .map(condicao -> {
                    if (condicao.getDescricao() != null && !condicao.getDescricao().isBlank()) {
                        return condicao.getTipo() + " (" + condicao.getDescricao() + ")";
                    }
                    return condicao.getTipo();
                })
                .filter(Objects::nonNull)
                .toList();

        if (condicoesAtivas.isEmpty()) {
            return "nenhuma";
        }
        return String.join(", ", condicoesAtivas);
    }

    private String valorOuNaoInformado(Object valor) {
        return valor == null ? "não informado" : valor.toString();
    }
}
