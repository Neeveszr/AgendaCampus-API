package com.faculdade.eventos.dto;

import com.faculdade.eventos.model.Participante;

import java.time.OffsetDateTime;

// Participante devolvido pela API.
public record ParticipanteResponse(
        Long id,
        String nome,
        String email,
        OffsetDateTime criadoEm
) {

    public static ParticipanteResponse de(Participante participante) {
        return new ParticipanteResponse(
                participante.getId(),
                participante.getNome(),
                participante.getEmail(),
                participante.getCriadoEm()
        );
    }
}
