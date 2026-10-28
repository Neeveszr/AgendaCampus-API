package com.faculdade.eventos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Entity
@Table(name = "participantes")
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "O e-mail deve ser válido")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Construtor vazio usado pelo JPA e pelo Jackson (que preenche só os campos enviados no JSON).
    public Participante() {
    }

    public Participante(String nome, String email) {
        this.nome = nome;
        setEmail(email);
    }

    @PrePersist
    private void antesDeSalvar() {
        this.criadoEm = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    // O JSON também passa por aqui, então o e-mail já chega normalizado na validação.
    public void setEmail(String email) {
        this.email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
