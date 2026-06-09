package com.seucorre.avaliacao.api;

import com.seucorre.avaliacao.application.AvaliacaoAppService;
import com.seucorre.avaliacao.application.dto.AnaliseRiscoDTO;
import com.seucorre.avaliacao.application.dto.CheckinSemanalRequest;
import com.seucorre.usuario.domain.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final AvaliacaoAppService service;

    @PostMapping
    public ResponseEntity<AnaliseRiscoDTO> processarCheckin(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestBody @Valid CheckinSemanalRequest request
    ) {
        AnaliseRiscoDTO analise = service.processarCheckin(usuarioLogado.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(analise);
    }

    @GetMapping("/historico")
    public ResponseEntity<List<AnaliseRiscoDTO>> listarHistorico(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(service.listarHistoricoCheckins(usuarioLogado.getId()));
    }
}
