package com.seucorre.usuario.application;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WearablePlatformRegistry {

    private final Map<PlataformaRelogio, WearableAdapter> adaptersByPlatform;

    public WearablePlatformRegistry(List<WearableAdapter> wearableAdapters) {
        this.adaptersByPlatform = wearableAdapters.stream()
                .collect(Collectors.toMap(
                        WearableAdapter::plataformaSuportada,
                        Function.identity(),
                        (existing, duplicate) -> {
                            throw new IllegalStateException("Mais de um adapter foi configurado para a mesma plataforma.");
                        },
                        LinkedHashMap::new
                ));
    }

    public boolean suporta(PlataformaRelogio plataforma) {
        return plataforma != null && adaptersByPlatform.containsKey(plataforma);
    }

    public Optional<WearableAdapter> localizarAdapter(PlataformaRelogio plataforma) {
        return Optional.ofNullable(adaptersByPlatform.get(plataforma));
    }

    public String descreverPlataformasDisponiveis() {
        if (adaptersByPlatform.isEmpty()) {
            return "nenhuma plataforma";
        }
        return adaptersByPlatform.keySet().stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(PlataformaRelogio::getDescricao)
                .collect(Collectors.joining(", "));
    }

    public String mensagemPlataformaNaoDisponivel(PlataformaRelogio plataforma) {
        String descricao = plataforma == null ? "informada" : plataforma.getDescricao();
        return "A plataforma " + descricao + " ainda não está disponível nesta versão. "
                + "Plataformas disponíveis: " + descreverPlataformasDisponiveis() + ".";
    }
}
