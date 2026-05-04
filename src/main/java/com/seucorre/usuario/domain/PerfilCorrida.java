package com.seucorre.usuario.domain;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.valueobjects.ZonaFC;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "perfil_corrida")
@Data
public class PerfilCorrida {

    private static final BigDecimal DISTANCIA_5K = BigDecimal.valueOf(5);
    private static final BigDecimal DISTANCIA_10K = BigDecimal.valueOf(10);
    private static final BigDecimal DISTANCIA_21K = BigDecimal.valueOf(21);
    private static final BigDecimal DISTANCIA_42K = BigDecimal.valueOf(42);
    private static final BigDecimal SEGUNDOS_POR_MINUTO = BigDecimal.valueOf(60);

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @Column(name = "pace_5k_min_km")
    private BigDecimal pace5kMinKm;

    @Column(name = "pace_10k_min_km")
    private BigDecimal pace10kMinKm;

    @Column(name = "pace_21k_min_km")
    private BigDecimal pace21kMinKm;

    @Column(name = "pace_42k_min_km")
    private BigDecimal pace42kMinKm;

    @Column(name = "vo2_estimado")
    private Integer vo2Estimado;

    @OneToMany(mappedBy = "perfilCorrida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ZonaFCPersistida> zonasFc = new ArrayList<>();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public void substituirZonas(List<ZonaFCPersistida> novasZonas) {
        zonasFc.clear();
        if (novasZonas == null) {
            return;
        }
        novasZonas.forEach(this::adicionarZona);
    }

    public void adicionarZona(ZonaFCPersistida zona) {
        zona.setPerfilCorrida(this);
        zonasFc.add(zona);
    }

    public Duration estimarTempoProva(double distanciaKm) {
        if (!Double.isFinite(distanciaKm) || distanciaKm <= 0) {
            throw new IllegalArgumentException("A distância deve ser positiva.");
        }

        BigDecimal paceEstimado = estimarPaceParaDistancia(BigDecimal.valueOf(distanciaKm));
        BigDecimal segundosTotais = paceEstimado
                .multiply(SEGUNDOS_POR_MINUTO)
                .multiply(BigDecimal.valueOf(distanciaKm))
                .setScale(0, RoundingMode.HALF_UP);

        return Duration.ofSeconds(segundosTotais.longValue());
    }

    public String classificarDesempenho() {
        if (vo2Estimado != null) {
            if (vo2Estimado < 35) {
                return NivelCondicionamento.INICIANTE.getDescricao();
            }
            if (vo2Estimado < 50) {
                return NivelCondicionamento.INTERMEDIARIO.getDescricao();
            }
            return NivelCondicionamento.AVANCADO.getDescricao();
        }

        BigDecimal melhorPace = obterMelhorPaceDisponivel();
        if (melhorPace == null) {
            return "Não classificado";
        }
        if (melhorPace.compareTo(BigDecimal.valueOf(5.0)) <= 0) {
            return NivelCondicionamento.AVANCADO.getDescricao();
        }
        if (melhorPace.compareTo(BigDecimal.valueOf(6.0)) <= 0) {
            return NivelCondicionamento.INTERMEDIARIO.getDescricao();
        }
        return NivelCondicionamento.INICIANTE.getDescricao();
    }

    public ZonaFC calcularZonaAtual(int fcAtual) {
        if (fcAtual <= 0) {
            throw new IllegalArgumentException("A frequência cardíaca atual deve ser positiva.");
        }
        if (zonasFc == null || zonasFc.isEmpty()) {
            throw new IllegalStateException("Zonas de frequência cardíaca não cadastradas.");
        }

        return zonasFc.stream()
                .filter(zona -> zona.getZona() != null
                        && zona.getFcMin() != null
                        && zona.getFcMax() != null)
                .filter(zona -> contemFrequencia(zona, fcAtual))
                .findFirst()
                .map(zona -> ZonaFC.of(zona.getZona(), zona.getFcMin(), zona.getFcMax()))
                .orElse(null);
    }

    private boolean contemFrequencia(ZonaFCPersistida zona, int fcAtual) {
        if (zona.getZona() != null && zona.getZona() == 5) {
            return fcAtual >= zona.getFcMin() && fcAtual <= zona.getFcMax();
        }
        return fcAtual >= zona.getFcMin() && fcAtual < zona.getFcMax();
    }

    private BigDecimal estimarPaceParaDistancia(BigDecimal distanciaKm) {
        List<PaceReferencia> referencias = obterPacesOrdenados();
        if (referencias.isEmpty()) {
            throw new IllegalStateException("Nenhum pace de referência foi informado.");
        }
        if (referencias.size() == 1) {
            return referencias.get(0).pace();
        }

        for (PaceReferencia referencia : referencias) {
            if (referencia.distanciaKm().compareTo(distanciaKm) == 0) {
                return referencia.pace();
            }
        }

        PaceReferencia primeira = referencias.get(0);
        PaceReferencia ultima = referencias.get(referencias.size() - 1);

        if (distanciaKm.compareTo(primeira.distanciaKm()) < 0) {
            return primeira.pace();
        }
        if (distanciaKm.compareTo(ultima.distanciaKm()) > 0) {
            return ultima.pace();
        }

        for (int i = 0; i < referencias.size() - 1; i++) {
            PaceReferencia atual = referencias.get(i);
            PaceReferencia proxima = referencias.get(i + 1);
            if (distanciaKm.compareTo(atual.distanciaKm()) > 0
                    && distanciaKm.compareTo(proxima.distanciaKm()) < 0) {
                return interpolarPace(atual, proxima, distanciaKm);
            }
        }

        return ultima.pace();
    }

    private BigDecimal interpolarPace(PaceReferencia origem, PaceReferencia destino, BigDecimal distanciaAlvo) {
        BigDecimal faixaDistancia = destino.distanciaKm().subtract(origem.distanciaKm());
        BigDecimal deslocamento = distanciaAlvo.subtract(origem.distanciaKm());
        BigDecimal variacaoPace = destino.pace().subtract(origem.pace());

        BigDecimal fator = deslocamento.divide(faixaDistancia, 8, RoundingMode.HALF_UP);
        return origem.pace().add(variacaoPace.multiply(fator));
    }

    private BigDecimal obterMelhorPaceDisponivel() {
        return obterPacesOrdenados().stream()
                .map(PaceReferencia::pace)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private List<PaceReferencia> obterPacesOrdenados() {
        List<PaceReferencia> referencias = new ArrayList<>();
        adicionarPaceSeInformado(referencias, DISTANCIA_5K, pace5kMinKm);
        adicionarPaceSeInformado(referencias, DISTANCIA_10K, pace10kMinKm);
        adicionarPaceSeInformado(referencias, DISTANCIA_21K, pace21kMinKm);
        adicionarPaceSeInformado(referencias, DISTANCIA_42K, pace42kMinKm);
        referencias.sort(Comparator.comparing(PaceReferencia::distanciaKm));
        return referencias;
    }

    private void adicionarPaceSeInformado(List<PaceReferencia> referencias,
                                          BigDecimal distanciaKm,
                                          BigDecimal pace) {
        if (pace != null && pace.signum() > 0) {
            referencias.add(new PaceReferencia(distanciaKm, pace));
        }
    }

    private record PaceReferencia(BigDecimal distanciaKm, BigDecimal pace) {
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
