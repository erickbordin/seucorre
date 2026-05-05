package com.seucorre.avaliacao.application;

import com.seucorre.avaliacao.application.dto.AnaliseRiscoDTO;
import com.seucorre.avaliacao.application.dto.CheckinSemanalRequest;
import com.seucorre.avaliacao.application.dto.ProgressoSemanalDTO;
import com.seucorre.avaliacao.application.event.PlanoReescritoEvent;
import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.avaliacao.infrastructure.CheckinRepository;
import com.seucorre.shared.domain.enums.NivelRisco;
import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.treino.domain.GeradorPlanoIA;
import com.seucorre.treino.domain.PlanoTreino;
import com.seucorre.treino.infrastructure.PlanoRepository;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvaliacaoAppService {

    private final ProgressoAppService progressoAppService;
    private final CheckinRepository checkinRepository;
    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeradorPlanoIA geradorPlanoIA;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AnaliseRiscoDTO processarCheckin(CheckinSemanalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de check-in é obrigatória.");
        }

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        PlanoTreino planoTreino = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new EntityNotFoundException("Plano de treino não encontrado."));

        if (planoTreino.getUsuario() == null || !usuario.getId().equals(planoTreino.getUsuario().getId())) {
            throw new BusinessRuleException("O plano informado não pertence ao usuário.");
        }

        CheckinSemanal checkinSemanal = checkinRepository.findByPlanoIdAndSemana(request.planoId(), request.semana())
                .orElseGet(CheckinSemanal::new);

        preencherCheckin(checkinSemanal, request, usuario, planoTreino);

        ProgressoSemanal progressoSemanal = progressoAppService.buscarProgressoSemana(planoTreino.getId(), request.semana());
        checkinSemanal.setAnaliseIA(gerarAnaliseIA(checkinSemanal, progressoSemanal));

        if (checkinSemanal.precisaReescreverPlano()) {
            PlanoTreino planoReescrito = geradorPlanoIA.reescreverPlano(planoTreino, checkinSemanal);
            persistirReescrita(planoTreino, planoReescrito, checkinSemanal);
        } else {
            checkinSemanal.setPlanoReescrito(false);
        }

        CheckinSemanal checkinSalvo = checkinRepository.save(checkinSemanal);
        return AnaliseRiscoDTO.from(checkinSalvo);
    }

    @Transactional
    public AnaliseRiscoDTO processarCheckin(UUID usuarioId, CheckinSemanalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request de check-in é obrigatória.");
        }
        if (!usuarioId.equals(request.usuarioId())) {
            throw new BusinessRuleException("O usuário informado no corpo da requisição difere do usuário autenticado.");
        }
        return processarCheckin(request);
    }

    @Transactional(readOnly = true)
    public List<AnaliseRiscoDTO> listarHistoricoCheckins(UUID usuarioId) {
        return checkinRepository.findByUsuarioIdOrderBySemanaAsc(usuarioId).stream()
                .map(AnaliseRiscoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressoSemanalDTO> listarHistoricoProgresso(UUID usuarioId) {
        return progressoAppService.listarHistoricoProgresso(usuarioId);
    }

    private void preencherCheckin(CheckinSemanal checkinSemanal,
                                  CheckinSemanalRequest request,
                                  Usuario usuario,
                                  PlanoTreino planoTreino) {
        checkinSemanal.setUsuario(usuario);
        checkinSemanal.setPlano(planoTreino);
        checkinSemanal.setSemana(request.semana());
        checkinSemanal.setDataCheckin(request.dataCheckin());
        checkinSemanal.setNivelEsforco(request.nivelEsforco());
        checkinSemanal.setNivelDor(request.nivelDor());
        checkinSemanal.setHorasSonoSemana(request.horasSonoSemana());
        checkinSemanal.setAvaliacao(request.avaliacao());
    }

    private String gerarAnaliseIA(CheckinSemanal checkinSemanal, ProgressoSemanal progressoSemanal) {
        String analiseCheckin = geradorPlanoIA.gerarAnaliseCheckin(checkinSemanal);
        if (progressoSemanal == null) {
            return analiseCheckin;
        }

        String alertaSemanal = progressoAppService.gerarAlertaSemanal(
                checkinSemanal.getPlano() == null ? null : checkinSemanal.getPlano().getId(),
                checkinSemanal.getSemana()
        );

        String sugestaoProgresso = geradorPlanoIA.sugerirAjuste(progressoSemanal);
        String analiseComSugestao = analiseCheckin;
        if (sugestaoProgresso != null && !sugestaoProgresso.isBlank()) {
            if (analiseComSugestao == null || analiseComSugestao.isBlank()) {
                analiseComSugestao = sugestaoProgresso;
            } else {
                analiseComSugestao = analiseComSugestao + "\nAjuste semanal: " + sugestaoProgresso;
            }
        }
        if (alertaSemanal == null || alertaSemanal.isBlank()) {
            return analiseComSugestao;
        }
        if (analiseComSugestao == null || analiseComSugestao.isBlank()) {
            return alertaSemanal;
        }
        return analiseComSugestao + "\n" + alertaSemanal;
    }

    private void persistirReescrita(PlanoTreino planoOriginal,
                                    PlanoTreino planoReescrito,
                                    CheckinSemanal checkinSemanal) {
        if (planoReescrito == null) {
            throw new BusinessRuleException("A IA não retornou um plano reescrito válido.");
        }

        checkinSemanal.setPlanoReescrito(true);
        planoOriginal.setStatus(StatusPlano.CANCELADO);
        planoRepository.save(planoOriginal);

        planoReescrito.setStatus(StatusPlano.ATIVO);
        PlanoTreino novoPlanoSalvo = planoRepository.save(planoReescrito);

        NivelRisco nivelRisco = checkinSemanal.avaliarNivelRisco();
        eventPublisher.publishEvent(new PlanoReescritoEvent(
                checkinSemanal.getUsuario() == null ? null : checkinSemanal.getUsuario().getId(),
                planoOriginal.getId(),
                novoPlanoSalvo.getId(),
                checkinSemanal.getId(),
                checkinSemanal.getSemana(),
                nivelRisco
        ));
    }
}
