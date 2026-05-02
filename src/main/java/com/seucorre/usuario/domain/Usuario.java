package com.seucorre.usuario.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
    @Column(name = "senha_hash")
    private String senhaHash;
    
    private String telefone;

    private String role; 

    @Column(name = "peso_kg")
    private BigDecimal pesoKg;

    @Column(name = "altura_cm")
    private BigDecimal alturaCm;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    private String genero;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_condicionamento")
    private NivelCondicionamento nivelCondicionamento;

    @Enumerated(EnumType.STRING)
    private Objetivo objetivo;

    @Column(name = "ja_corre")
    private Boolean jaCorre;

    private Boolean sedentario;

    @Column(name = "horas_sono_media")
    private Integer horasSonoMedia;

    @Column(name = "dias_disponiveis_semana")
    private Integer diasDisponiveisSemana;

    @Column(name = "dias_semana_treino")
    private String diasSemanaTreino;

    @Column(name = "fc_repouso")
    private Integer fcRepouso;

    @Column(name = "fc_maxima")
    private Integer fcMaxima;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PerfilCorrida perfilCorrida;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CondicaoSaude> condicoesSaude = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Relogio> relogios = new ArrayList<>();

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public boolean estaAptoParaTreinar() {
        return nome != null && !nome.isBlank()
                && email != null && !email.isBlank()
                && pesoKg != null
                && alturaCm != null
                && nivelCondicionamento != null
                && objetivo != null
                && !possuiCondicaoRestritiva();
    }

    public void definirPerfilCorrida(PerfilCorrida perfilCorrida) {
        if (perfilCorrida == null) {
            this.perfilCorrida = null;
            return;
        }
        perfilCorrida.setUsuario(this);
        this.perfilCorrida = perfilCorrida;
    }

    public void substituirCondicoesSaude(List<CondicaoSaude> novasCondicoes) {
        condicoesSaude.clear();
        if (novasCondicoes == null) {
            return;
        }
        novasCondicoes.forEach(this::adicionarCondicaoSaude);
    }

    public void adicionarCondicaoSaude(CondicaoSaude condicaoSaude) {
        condicaoSaude.setUsuario(this);
        condicoesSaude.add(condicaoSaude);
    }

    public void substituirRelogios(List<Relogio> novosRelogios) {
        relogios.clear();
        if (novosRelogios == null) {
            return;
        }
        novosRelogios.forEach(this::vincularRelogio);
    }

    public void vincularRelogio(Relogio relogio) {
        relogio.setUsuario(this);
        relogios.add(relogio);
    }

    public int calcularFcMaxTeorica() {
        if (fcMaxima != null) {
            return fcMaxima;
        }
        if (dataNascimento == null) {
            return 0;
        }
        int idade = LocalDate.now().getYear() - dataNascimento.getYear();
        return Math.max(0, 220 - idade);
    }

    private boolean possuiCondicaoRestritiva() {
        return condicoesSaude.stream().anyMatch(CondicaoSaude::restringeAtividade);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (role == null || role.isBlank()) {
            role = "USER";
        }
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String normalizedRole = role == null || role.isBlank() ? "USER" : role;
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
    }

    @Override
    public String getPassword() {
        return senhaHash;
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

