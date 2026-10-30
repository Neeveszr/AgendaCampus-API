package com.faculdade.eventos.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, length = 200)
    private String local;

    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio;

    @Column(name = "prazo_inscricao", nullable = false)
    private OffsetDateTime prazoInscricao;

    @Column(nullable = false)
    private Integer capacidade;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Lado inverso do relacionamento: a chave estrangeira fica em Inscricao.evento.
    @OneToMany(mappedBy = "evento")
    private List<Inscricao> inscricoes = new ArrayList<>();

    // Construtor vazio exigido pelo JPA.
    protected Evento() {
    }

    public Evento(
            String nome,
            String descricao,
            String local,
            OffsetDateTime dataInicio,
            OffsetDateTime prazoInscricao,
            Integer capacidade
    ) {
        this.nome = nome;
        this.descricao = descricao;
        this.local = local;
        this.dataInicio = dataInicio;
        this.prazoInscricao = prazoInscricao;
        this.capacidade = capacidade;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public OffsetDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(OffsetDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public OffsetDateTime getPrazoInscricao() {
        return prazoInscricao;
    }

    public void setPrazoInscricao(OffsetDateTime prazoInscricao) {
        this.prazoInscricao = prazoInscricao;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<Inscricao> getInscricoes() {
        return inscricoes;
    }
}
