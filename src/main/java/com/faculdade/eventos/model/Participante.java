package com.faculdade.eventos.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participantes")
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Lado inverso do relacionamento: a chave estrangeira fica em Inscricao.participante.
    @OneToMany(mappedBy = "participante")
    private List<Inscricao> inscricoes = new ArrayList<>();

    // Construtor vazio exigido pelo JPA.
    protected Participante() {
    }

    public Participante(String nome, String email) {
        this.nome = nome;
        this.email = email;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<Inscricao> getInscricoes() {
        return inscricoes;
    }
}
