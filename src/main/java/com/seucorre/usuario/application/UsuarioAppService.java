package com.seucorre.usuario.application;

import com.seucorre.shared.util.UuidGenerator;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioAppService {

    private final UsuarioRepository repository;

    public Usuario registrar(UsuarioCadastroRequest request) {
        Usuario usuario = new Usuario();
        usuario.setId(UuidGenerator.generate());
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(request.senha());
        return repository.save(usuario);
    }
}