package com.faculdade.eventos.model;

import jakarta.persistence.*;

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

    // Lado dono do relacionamento com Evento (@OneToMany em Evento.inscricoes).
    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    // Lado dono do relacionamento com Participante (@OneToMany em Participante.inscricoes).
    @ManyToOne(optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    private Participante participante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusInscricao status;

    @Column(name = "data_inscricao", nullable = false, updatable = false)
    private OffsetDateTime dataInscricao;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    // Construtor vazio exigido pelo JPA.
    protected Inscricao() {
    }

    // Toda inscrição nova começa confirmada.
    public Inscricao(Evento evento, Participante participante) {
        this.evento = evento;
        this.participante = participante;
        this.status = StatusInscricao.CONFIRMADA;
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

    public Participante getParticipante() {
        return participante;
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
