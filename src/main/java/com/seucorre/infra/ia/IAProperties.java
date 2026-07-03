package com.seucorre.infra.ia;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "seucorre.ia")
public class IAProperties {

    private Provider provider = Provider.MANUAL;

    public boolean usaModoManual() {
        return provider == Provider.MANUAL;
    }

    public enum Provider {
        MANUAL, OLLAMA, ANTHROPIC
    }
}
