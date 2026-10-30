package com.faculdade.eventos.dto;

import com.faculdade.eventos.model.Evento;

import java.time.OffsetDateTime;

// Dados do evento exibidos dentro de uma inscrição.
public record EventoResumoResponse(
        Long id,
        String nome,
        String local,
        OffsetDateTime dataInicio,
        OffsetDateTime prazoInscricao
) {

    public static EventoResumoResponse de(Evento evento) {
        return new EventoResumoResponse(
                evento.getId(),
                evento.getNome(),
                evento.getLocal(),
                evento.getDataInicio(),
                evento.getPrazoInscricao()
        );
    }
}
