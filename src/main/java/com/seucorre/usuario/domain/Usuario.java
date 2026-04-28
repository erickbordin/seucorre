package com.seucorre.usuario.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class Usuario {
    
    @Id
    private UUID id;
    
    private String nome;
    
    @Column(unique = true)
    private String email;
    
    private String senha;
    
    private String role; 
    
    private LocalDateTime createdAt = LocalDateTime.now();

    
    public boolean estaAptoParaTreinar() {
        
        return nome != null && !nome.isEmpty();
    }
}