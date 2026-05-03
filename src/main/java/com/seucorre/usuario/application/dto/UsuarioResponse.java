package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.usuario.domain.CondicaoSaude;
import com.seucorre.usuario.domain.DispositivoExterno;
import com.seucorre.usuario.domain.PerfilCorrida;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.domain.ZonaFCPersistida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        BigDecimal pesoKg,
        BigDecimal alturaCm,
        LocalDate dataNascimento,
        String genero,
        NivelCondicionamento nivelCondicionamento,
        Objetivo objetivo,
        Boolean jaCorre,
        Boolean sedentario,
        Integer horasSonoMedia,
        Integer diasDisponiveisSemana,
        String diasSemanaTreino,
        Integer fcRepouso,
        Integer fcMaxima,
        boolean aptoParaTreinar,
        PerfilCorridaResponse perfilCorrida,
        List<CondicaoSaudeResponse> condicoesSaude,
        List<DispositivoExternoResponse> dispositivos,
        LocalDate criadoEm
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPesoKg(),
                usuario.getAlturaCm(),
                usuario.getDataNascimento(),
                usuario.getGenero(),
                usuario.getNivelCondicionamento(),
                usuario.getObjetivo(),
                usuario.getJaCorre(),
                usuario.getSedentario(),
                usuario.getHorasSonoMedia(),
                usuario.getDiasDisponiveisSemana(),
                usuario.getDiasSemanaTreino(),
                usuario.getFcRepouso(),
                usuario.getFcMaxima(),
                usuario.estaAptoParaTreinar(),
                PerfilCorridaResponse.from(usuario.getPerfilAtleta() == null ? null : usuario.getPerfilAtleta().getPerfilCorrida()),
                usuario.getCondicoesSaude().stream().map(CondicaoSaudeResponse::from).toList(),
                usuario.getDispositivos().stream().map(DispositivoExternoResponse::from).toList(),
                usuario.getCriadoEm()
        );
    }

    public record PerfilCorridaResponse(
            BigDecimal pace5kMinKm,
            BigDecimal pace10kMinKm,
            BigDecimal pace21kMinKm,
            BigDecimal pace42kMinKm,
            Integer vo2Estimado,
            List<ZonaFCResponse> zonasFc
    ) {
        static PerfilCorridaResponse from(PerfilCorrida perfilCorrida) {
            if (perfilCorrida == null) {
                return null;
            }
            return new PerfilCorridaResponse(
                    perfilCorrida.getPace5kMinKm(),
                    perfilCorrida.getPace10kMinKm(),
                    perfilCorrida.getPace21kMinKm(),
                    perfilCorrida.getPace42kMinKm(),
                    perfilCorrida.getVo2Estimado(),
                    perfilCorrida.getZonasFc().stream().map(ZonaFCResponse::from).toList()
            );
        }
    }

    public record ZonaFCResponse(Integer zona, String nome, Integer fcMin, Integer fcMax, Boolean personalizada) {
        static ZonaFCResponse from(ZonaFCPersistida zona) {
            return new ZonaFCResponse(
                    zona.getZona(),
                    zona.getNome(),
                    zona.getFcMin(),
                    zona.getFcMax(),
                    zona.getPersonalizada()
            );
        }
    }

    public record CondicaoSaudeResponse(String tipo, String descricao, Boolean ativa) {
        static CondicaoSaudeResponse from(CondicaoSaude condicaoSaude) {
            return new CondicaoSaudeResponse(
                    condicaoSaude.getTipo(),
                    condicaoSaude.getDescricao(),
                    condicaoSaude.getAtiva()
            );
        }
    }

    public record DispositivoExternoResponse(PlataformaRelogio plataforma, boolean tokenValido) {
        static DispositivoExternoResponse from(DispositivoExterno dispositivoExterno) {
            return new DispositivoExternoResponse(dispositivoExterno.getPlataforma(), dispositivoExterno.tokenValido());
        }
    }
}
