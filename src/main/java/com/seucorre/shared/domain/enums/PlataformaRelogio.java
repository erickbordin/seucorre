package com.seucorre.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlataformaRelogio {

    GARMIN("Garmin Connect"),
    POLAR("Polar Flow"),
    APPLE_WATCH("Apple Watch"),
    STRAVA("Strava");

    private final String descricao;
}
