package com.seucorre.usuario.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seucorre.shared.domain.valueobjects.IMC;
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

    @Embedded
    private DadosFisicos dadosFisicos;

    @Embedded
    private PerfilAtleta perfilAtleta;

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
    private List<DispositivoExterno> dispositivos = new ArrayList<>();

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public boolean estaAptoParaTreinar() {
        return nome != null && !nome.isBlank()
                && email != null && !email.isBlank()
                && getPesoKg() != null
                && getAlturaCm() != null
                && getNivelCondicionamento() != null
                && getObjetivo() != null
                && !possuiCondicaoRestritiva();
    }

    public void atualizarDadosFisicos(DadosFisicos dadosFisicos) {
        this.dadosFisicos = dadosFisicos;
    }

    public void atualizarPerfilAtleta(PerfilAtleta perfilAtleta) {
        if (perfilAtleta != null) {
            perfilAtleta.atualizarPerfilCorrida(this.perfilCorrida);
        }
        this.perfilAtleta = perfilAtleta;
    }

    public void definirPerfilCorrida(PerfilCorrida perfilCorrida) {
        if (perfilCorrida == null) {
            this.perfilCorrida = null;
            return;
        }
        perfilCorrida.setUsuario(this);
        this.perfilCorrida = perfilCorrida;
        if (perfilAtleta != null) {
            perfilAtleta.atualizarPerfilCorrida(perfilCorrida);
        }
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

    public void substituirDispositivos(List<DispositivoExterno> novosDispositivos) {
        dispositivos.clear();
        if (novosDispositivos == null) {
            return;
        }
        novosDispositivos.forEach(this::vincularDispositivo);
    }

    public void vincularDispositivo(DispositivoExterno dispositivoExterno) {
        dispositivoExterno.setUsuario(this);
        dispositivos.add(dispositivoExterno);
    }

    public IMC calcularIMC() {
        if (dadosFisicos == null) {
            throw new IllegalStateException("Dados físicos não informados.");
        }
        return dadosFisicos.calcularIMC();
    }

    public int calcularFcMaxTeorica() {
        if (dadosFisicos == null) {
            return 0;
        }
        Integer fcMaxima = dadosFisicos.getFcMaxima();
        if (fcMaxima != null) {
            return fcMaxima;
        }
        Integer idade = dadosFisicos.getIdade();
        if (idade == null) {
            return 0;
        }
        return Math.max(0, 220 - idade);
    }

    public String gerarResumoParaIA() {
        if (perfilAtleta == null) {
            return dadosFisicos == null ? "Perfil do atleta não informado." : dadosFisicos.gerarResumoMetricasParaIA() + ".";
        }
        return perfilAtleta.gerarResumoParaIA(dadosFisicos, perfilCorrida, condicoesSaude);
    }

    private boolean possuiCondicaoRestritiva() {
        return condicoesSaude.stream().anyMatch(CondicaoSaude::restringeAtividade);
    }

    public BigDecimal getPesoKg() {
        return dadosFisicos == null ? null : dadosFisicos.getPesoKg();
    }

    public void setPesoKg(BigDecimal pesoKg) {
        obterOuCriarDadosFisicos().setPesoKg(pesoKg);
    }

    public BigDecimal getAlturaCm() {
        return dadosFisicos == null ? null : dadosFisicos.getAlturaCm();
    }

    public void setAlturaCm(BigDecimal alturaCm) {
        obterOuCriarDadosFisicos().setAlturaCm(alturaCm);
    }

    public LocalDate getDataNascimento() {
        return dadosFisicos == null ? null : dadosFisicos.getDataNascimento();
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        obterOuCriarDadosFisicos().setDataNascimento(dataNascimento);
    }

    public String getGenero() {
        return dadosFisicos == null ? null : dadosFisicos.getGenero();
    }

    public void setGenero(String genero) {
        obterOuCriarDadosFisicos().setGenero(genero);
    }

    public Boolean getSedentario() {
        return dadosFisicos == null ? null : dadosFisicos.getSedentario();
    }

    public void setSedentario(Boolean sedentario) {
        obterOuCriarDadosFisicos().setSedentario(sedentario);
    }

    public Integer getHorasSonoMedia() {
        return dadosFisicos == null ? null : dadosFisicos.getHorasSonoMedia();
    }

    public void setHorasSonoMedia(Integer horasSonoMedia) {
        obterOuCriarDadosFisicos().setHorasSonoMedia(horasSonoMedia);
    }

    public Integer getFcRepouso() {
        return dadosFisicos == null ? null : dadosFisicos.getFcRepouso();
    }

    public void setFcRepouso(Integer fcRepouso) {
        obterOuCriarDadosFisicos().setFcRepouso(fcRepouso);
    }

    public Integer getFcMaxima() {
        return dadosFisicos == null ? null : dadosFisicos.getFcMaxima();
    }

    public void setFcMaxima(Integer fcMaxima) {
        obterOuCriarDadosFisicos().setFcMaxima(fcMaxima);
    }

    public NivelCondicionamento getNivelCondicionamento() {
        return perfilAtleta == null ? null : perfilAtleta.getNivelCondicionamento();
    }

    public void setNivelCondicionamento(NivelCondicionamento nivelCondicionamento) {
        obterOuCriarPerfilAtleta().setNivelCondicionamento(nivelCondicionamento);
    }

    public Objetivo getObjetivo() {
        return perfilAtleta == null ? null : perfilAtleta.getObjetivo();
    }

    public void setObjetivo(Objetivo objetivo) {
        obterOuCriarPerfilAtleta().setObjetivo(objetivo);
    }

    public Boolean getJaCorre() {
        return perfilAtleta == null ? null : perfilAtleta.getJaCorre();
    }

    public void setJaCorre(Boolean jaCorre) {
        obterOuCriarPerfilAtleta().setJaCorre(jaCorre);
    }

    public Integer getDiasDisponiveisSemana() {
        return perfilAtleta == null ? null : perfilAtleta.getDiasDisponiveisSemana();
    }

    public void setDiasDisponiveisSemana(Integer diasDisponiveisSemana) {
        obterOuCriarPerfilAtleta().setDiasDisponiveisSemana(diasDisponiveisSemana);
    }

    public String getDiasSemanaTreino() {
        return perfilAtleta == null ? null : perfilAtleta.getDiasSemanaTreino();
    }

    public void setDiasSemanaTreino(String diasSemanaTreino) {
        obterOuCriarPerfilAtleta().setDiasSemanaTreino(diasSemanaTreino);
    }

    private DadosFisicos obterOuCriarDadosFisicos() {
        if (dadosFisicos == null) {
            dadosFisicos = new DadosFisicos();
        }
        return dadosFisicos;
    }

    private PerfilAtleta obterOuCriarPerfilAtleta() {
        if (perfilAtleta == null) {
            perfilAtleta = new PerfilAtleta();
        }
        return perfilAtleta;
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

