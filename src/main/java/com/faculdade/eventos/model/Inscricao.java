package com.faculdade.eventos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
        name = "inscricoes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inscricoes_evento_participante",
                        columnNames = {"evento_id", "participante_id"}
                )
        }
)
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // As vagas não são calculadas aqui dentro; por isso o campo fica fora do JSON da inscrição.
    @JsonIgnoreProperties("vagasDisponiveis")
    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    private Participante participante;

    @NotNull(message = "O status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusInscricao status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "data_inscricao", nullable = false, updatable = false)
    private OffsetDateTime dataInscricao;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    // Construtor vazio usado pelo JPA e pelo Jackson (que preenche só os campos enviados no JSON).
    public Inscricao() {
    }

    @PrePersist
    private void antesDeSalvar() {
        OffsetDateTime agora = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        this.dataInscricao = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    private void antesDeAtualizar() {
        this.atualizadoEm = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Participante getParticipante() {
        return participante;
    }

    public void setParticipante(Participante participante) {
        this.participante = participante;
    }

    public StatusInscricao getStatus() {
        return status;
    }

    public void setStatus(StatusInscricao status) {
        this.status = status;
    }

    public OffsetDateTime getDataInscricao() {
        return dataInscricao;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
