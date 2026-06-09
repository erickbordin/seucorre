package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoTreino {
    
    INTERVALADO("Alternância entre explosões de alta intensidade e períodos de descanso, ideal para ganhar velocidade e potência.", 9),
    FARTLAKE("Mistura velocidades diferentes de forma livre e contínua, alternando entre trotes leves e acelerações intensas sem pausas totais.", 8),
    LONGO("Sessão de maior quilometragem realizada em ritmo lento para preparar o organismo para grandes distâncias.", 7),
    REGENERATIVO("Corrida de curta duração e intensidade leve, voltada exclusivamente para a recuperação muscular ativa.", 5);

    private final String descricao;
    private final int fatorIntensidade;
}