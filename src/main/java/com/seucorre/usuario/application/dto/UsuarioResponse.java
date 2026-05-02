package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.usuario.domain.CondicaoSaude;
import com.seucorre.usuario.domain.PerfilCorrida;
import com.seucorre.usuario.domain.Relogio;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.domain.ZonaFCPersistida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
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
        List<RelogioResponse> relogios,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
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
                PerfilCorridaResponse.from(usuario.getPerfilCorrida()),
                usuario.getCondicoesSaude().stream().map(CondicaoSaudeResponse::from).toList(),
                usuario.getRelogios().stream().map(RelogioResponse::from).toList(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
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

    public record RelogioResponse(String plataforma, boolean tokenValido) {
        static RelogioResponse from(Relogio relogio) {
            return new RelogioResponse(relogio.getPlataforma(), relogio.tokenValido());
        }
    }
}
