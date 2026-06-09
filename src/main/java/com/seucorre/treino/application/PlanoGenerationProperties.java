package com.seucorre.treino.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "seucorre.treino.geracao")
public class PlanoGenerationProperties {

    private Mode mode = Mode.PRESET;
    private boolean fallbackToIa = false;

    public boolean usaPreset() {
        return mode == Mode.PRESET;
    }

    public enum Mode {
        PRESET,
        IA
    }
}
