package com.seucorre.usuario.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users") // Mantendo o nome da tabela que criamos no Docker/Flyway
@Data
public class Usuario {
    
    @Id
    private UUID id; // Mudamos de String para UUID
    
    private String nome;
    
    @Column(unique = true)
    private String email;
    
    private String senha;
    
    private String role; 
    
    private LocalDateTime createdAt = LocalDateTime.now();

    // Métodos de domínio (Regras de negócio que você definiu no mapa)
    public boolean estaAptoParaTreinar() {
        // Lógica inicial: se tem nome e email, está apto (vamos evoluir isso)
        return nome != null && !nome.isEmpty();
    }
}