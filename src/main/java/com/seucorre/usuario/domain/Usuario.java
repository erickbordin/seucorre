package com.seucorre.usuario.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
@Data
public class Usuario implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    private String nome;
    
    @Column(unique = true)
    private String email;
    
    @JsonIgnore
    private String senha;
    
    private String role; 
    
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean estaAptoParaTreinar() {
      
        return nome != null && !nome.isEmpty();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // No futuro você pode usar o campo 'role', por enquanto retornamos um padrão
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

