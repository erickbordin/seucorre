package com.seucorre.infra.wearables;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.usuario.application.WearableAdapter;
import com.seucorre.usuario.domain.DispositivoExterno;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StravaWearableAdapter implements WearableAdapter {

    @Override
    public PlataformaRelogio plataformaSuportada() {
        return PlataformaRelogio.STRAVA;
    }

    @Override
    public List<AtividadeExterna> sincronizarDados(DispositivoExterno dispositivoExterno) {
        return List.of();
    }
}
