package com.seucorre.infra.ia;

import com.seucorre.shared.exception.IAUnavailableException;
import com.seucorre.treino.domain.IAClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "seucorre.ia.provider", havingValue = "manual", matchIfMissing = true)
public class ManualOnlyIAClient implements IAClient {

    @Override
    public String gerarResposta(String prompt) {
        throw new IAUnavailableException(
                "O SeuCorre esta configurado em modo manual e nao possui provider de IA habilitado."
        );
    }
}
