package com.faculdade.eventos.dto;

import com.faculdade.eventos.model.Evento;

import java.time.OffsetDateTime;

// Evento devolvido pela API, com as vagas disponíveis já calculadas.
public record EventoResponse(
        Long id,
        String nome,
        String descricao,
        String local,
        OffsetDateTime dataInicio,
        OffsetDateTime prazoInscricao,
        Integer capacidade,
        long vagasDisponiveis,
        OffsetDateTime criadoEm
) {

    // As vagas não ficam na entidade; o service calcula e informa aqui.
    public static EventoResponse de(Evento evento, long vagasDisponiveis) {
        return new EventoResponse(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getLocal(),
                evento.getDataInicio(),
                evento.getPrazoInscricao(),
                evento.getCapacidade(),
                vagasDisponiveis,
                evento.getCriadoEm()
        );
    }
}
