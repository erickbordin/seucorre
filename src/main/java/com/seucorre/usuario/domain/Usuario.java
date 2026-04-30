package com.seucorre.usuario.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity
@Table(name = "usuario")
@Data
public class Usuario implements UserDetails {
    
    @Id
    private UUID id;
    
    private String nome;
    
    @Column(unique = true)
    private String email;
    
    @JsonIgnore
    private String senha;
    
    // --- Dados Físicos (Flat) ---
    private BigDecimal peso;
    
    private BigDecimal altura;
    
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    // --- Perfil Atleta (Flat) ---
    private String experiencia;
    
    private String objetivo;
    
    @Column(name = "fc_maxima")
    private Integer fcMaxima;
    
    // --- Pace Alvo (Flat) ---
    @Column(name = "pace_alvo_minutos")
    private Integer paceAlvoMinutos;
    
    @Column(name = "pace_alvo_segundos")
    private Integer paceAlvoSegundos;

    // --- Auditoria ---
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public boolean estaAptoParaTreinar() {
        return nome != null && !nome.isEmpty();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}