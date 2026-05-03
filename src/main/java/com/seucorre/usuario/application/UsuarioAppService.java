package com.seucorre.usuario.application;

import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.shared.util.UuidGenerator;
import com.seucorre.usuario.application.dto.CondicaoSaudeRequest;
import com.seucorre.usuario.application.dto.DadosFisicosRequest;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.OnboardingRequest;
import com.seucorre.usuario.application.dto.PerfilAtletaRequest;
import com.seucorre.usuario.application.dto.PerfilCorridaRequest;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.dto.UsuarioResponse;
import com.seucorre.usuario.application.dto.ZonaFCRequest;
import com.seucorre.usuario.domain.CondicaoSaude;
import com.seucorre.usuario.domain.DadosFisicos;
import com.seucorre.usuario.domain.DispositivoExterno;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.PerfilCorrida;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.domain.ZonaFCPersistida;
import com.seucorre.usuario.infrastructure.PerfilCorridaRepository;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioAppService {

    private final UsuarioRepository repository;
    private final PerfilCorridaRepository perfilCorridaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse registrar(UsuarioCadastroRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new BusinessRuleException("E-mail já cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setId(UuidGenerator.generateUuid());
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));

        PerfilCorrida perfilCorrida = toPerfilCorrida(request.perfilCorrida());

        aplicarOnboarding(usuario, new OnboardingRequest(
                request.telefone(),
                request.dadosFisicos(),
                request.perfilAtleta(),
                request.perfilCorrida(),
                request.condicoesSaude(),
                request.dispositivos()
        ), perfilCorrida);

        Usuario usuarioSalvo = repository.save(usuario);
        PerfilCorrida perfilCorridaSalvo = salvarPerfilCorrida(usuarioSalvo, perfilCorrida);
        anexarPerfilCorrida(usuarioSalvo, perfilCorridaSalvo);
        return UsuarioResponse.from(usuarioSalvo);
    }

    @Transactional
    public UsuarioResponse atualizarOnboarding(java.util.UUID usuarioId, OnboardingRequest request) {
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        PerfilCorrida perfilCorrida = mergePerfilCorrida(usuario, request.perfilCorrida());
        aplicarOnboarding(usuario, request, perfilCorrida);

        Usuario usuarioSalvo = repository.save(usuario);
        PerfilCorrida perfilCorridaSalvo = salvarPerfilCorrida(usuarioSalvo, perfilCorrida);
        anexarPerfilCorrida(usuarioSalvo, perfilCorridaSalvo);
        return UsuarioResponse.from(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return repository.findAll().stream()
                .peek(usuario -> anexarPerfilCorrida(usuario, buscarPerfilCorrida(usuario)))
                .map(UsuarioResponse::from)
                .toList();
    }

    private void aplicarOnboarding(Usuario usuario, OnboardingRequest request, PerfilCorrida perfilCorrida) {
        if (request == null) {
            return;
        }

        aplicarDadosFisicos(usuario, request.dadosFisicos());
        aplicarPerfilAtleta(usuario, request.perfilAtleta());
        usuario.substituirCondicoesSaude(toCondicoesSaude(request.condicoesSaude()));
        usuario.substituirDispositivos(toDispositivos(request.dispositivos()));
        anexarPerfilCorrida(usuario, perfilCorrida);
        validarOnboarding(usuario, perfilCorrida);
    }

    private void aplicarDadosFisicos(Usuario usuario, DadosFisicosRequest dadosFisicos) {
        if (dadosFisicos == null) {
            return;
        }
        usuario.atualizarDadosFisicos(toDadosFisicos(dadosFisicos));
    }

    private void aplicarPerfilAtleta(Usuario usuario, PerfilAtletaRequest perfilAtleta) {
        if (perfilAtleta == null) {
            return;
        }
        usuario.atualizarPerfilAtleta(toPerfilAtleta(perfilAtleta));
    }

    private DadosFisicos toDadosFisicos(DadosFisicosRequest request) {
        DadosFisicos dadosFisicos = new DadosFisicos();
        dadosFisicos.setPesoKg(request.pesoKg());
        dadosFisicos.setAlturaCm(request.alturaCm());
        dadosFisicos.setDataNascimento(request.dataNascimento());
        dadosFisicos.setGenero(request.genero());
        dadosFisicos.setFcRepouso(request.fcRepouso());
        dadosFisicos.setFcMaxima(request.fcMaxima());
        dadosFisicos.setHorasSonoMedia(request.horasSonoMedia());
        dadosFisicos.setSedentario(request.sedentario());
        return dadosFisicos;
    }

    private PerfilAtleta toPerfilAtleta(PerfilAtletaRequest request) {
        PerfilAtleta perfilAtleta = new PerfilAtleta();
        perfilAtleta.setNivelCondicionamento(request.nivelCondicionamento());
        perfilAtleta.setObjetivo(request.objetivo());
        perfilAtleta.setJaCorre(request.jaCorre());
        perfilAtleta.setDiasDisponiveisSemana(request.diasDisponiveisSemana());
        perfilAtleta.setDiasSemanaTreino(request.diasSemanaTreino());
        return perfilAtleta;
    }

    private PerfilCorrida toPerfilCorrida(PerfilCorridaRequest request) {
        if (request == null) {
            return null;
        }
        PerfilCorrida perfilCorrida = new PerfilCorrida();
        perfilCorrida.setPace5kMinKm(request.pace5kMinKm());
        perfilCorrida.setPace10kMinKm(request.pace10kMinKm());
        perfilCorrida.setPace21kMinKm(request.pace21kMinKm());
        perfilCorrida.setPace42kMinKm(request.pace42kMinKm());
        perfilCorrida.setVo2Estimado(request.vo2Estimado());
        perfilCorrida.substituirZonas(toZonas(request.zonasFc()));
        return perfilCorrida;
    }

    private List<ZonaFCPersistida> toZonas(List<ZonaFCRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(request -> {
            ZonaFCPersistida zona = new ZonaFCPersistida();
            zona.setZona(request.zona());
            zona.setNome(request.nome());
            zona.setFcMin(request.fcMin());
            zona.setFcMax(request.fcMax());
            zona.setPersonalizada(Boolean.TRUE.equals(request.personalizada()));
            return zona;
        }).toList();
    }

    private List<CondicaoSaude> toCondicoesSaude(List<CondicaoSaudeRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(request -> {
            CondicaoSaude condicaoSaude = new CondicaoSaude();
            condicaoSaude.setTipo(request.tipo());
            condicaoSaude.setDescricao(request.descricao());
            condicaoSaude.setAtiva(request.ativa() == null || request.ativa());
            return condicaoSaude;
        }).toList();
    }

    private List<DispositivoExterno> toDispositivos(List<DispositivoExternoRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(request -> {
            DispositivoExterno dispositivoExterno = new DispositivoExterno();
            dispositivoExterno.setPlataforma(request.plataforma());
            dispositivoExterno.setTokenAcesso(request.tokenAcesso());
            dispositivoExterno.setTokenExpiresAt(request.tokenExpiresAt());
            return dispositivoExterno;
        }).toList();
    }

    private void validarOnboarding(Usuario usuario, PerfilCorrida perfilCorrida) {
        if (usuario.getDiasDisponiveisSemana() != null
                && usuario.getDiasSemanaTreino() != null
                && !usuario.getDiasSemanaTreino().isBlank()) {
            long diasInformados = usuario.getDiasSemanaTreino().split(",").length;
            if (diasInformados > usuario.getDiasDisponiveisSemana()) {
                throw new BusinessRuleException("Dias de treino não podem exceder os dias disponíveis na semana.");
            }
        }

        if (perfilCorrida != null) {
            perfilCorrida.getZonasFc().forEach(zona -> {
                if (zona.getFcMin() != null && zona.getFcMax() != null && zona.getFcMax() < zona.getFcMin()) {
                    throw new BusinessRuleException("FC máxima da zona deve ser maior ou igual à FC mínima.");
                }
            });
        }
    }

    private PerfilCorrida mergePerfilCorrida(Usuario usuario, PerfilCorridaRequest request) {
        PerfilCorrida perfilExistente = buscarPerfilCorrida(usuario);
        if (request == null) {
            return perfilExistente;
        }

        PerfilCorrida perfilCorrida = toPerfilCorrida(request);
        if (perfilExistente != null) {
            perfilCorrida.setId(perfilExistente.getId());
        }
        return perfilCorrida;
    }

    private PerfilCorrida salvarPerfilCorrida(Usuario usuario, PerfilCorrida perfilCorrida) {
        if (perfilCorrida == null) {
            perfilCorridaRepository.findByUsuarioId(usuario.getId()).ifPresent(perfilCorridaRepository::delete);
            return null;
        }
        perfilCorrida.setUsuario(usuario);
        return perfilCorridaRepository.save(perfilCorrida);
    }

    private PerfilCorrida buscarPerfilCorrida(Usuario usuario) {
        if (usuario.getId() == null) {
            return null;
        }
        return perfilCorridaRepository.findByUsuarioId(usuario.getId()).orElse(null);
    }

    private void anexarPerfilCorrida(Usuario usuario, PerfilCorrida perfilCorrida) {
        if (usuario.getPerfilAtleta() != null) {
            usuario.getPerfilAtleta().atualizarPerfilCorrida(perfilCorrida);
        }
    }
}
