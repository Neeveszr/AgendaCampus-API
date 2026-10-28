package com.faculdade.eventos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotBlank(message = "O local é obrigatório")
    @Size(max = 200, message = "O local deve ter no máximo 200 caracteres")
    @Column(nullable = false, length = 200)
    private String local;

    @NotNull(message = "A data de início é obrigatória")
    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio;

    @NotNull(message = "O prazo de inscrição é obrigatório")
    @Column(name = "prazo_inscricao", nullable = false)
    private OffsetDateTime prazoInscricao;

    @NotNull(message = "A capacidade é obrigatória")
    @Min(value = 1, message = "A capacidade deve ser maior que zero")
    @Column(nullable = false)
    private Integer capacidade;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Não é uma coluna: o service calcula com base nas inscrições confirmadas.
    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long vagasDisponiveis;

    // Construtor vazio usado pelo JPA e pelo Jackson (que preenche só os campos enviados no JSON).
    public Evento() {
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

    public long getVagasDisponiveis() {
        return vagasDisponiveis;
    }

    public void setVagasDisponiveis(long vagasDisponiveis) {
        this.vagasDisponiveis = vagasDisponiveis;
    }
}
