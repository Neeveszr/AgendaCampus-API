package com.faculdade.eventos.dto;

import com.faculdade.eventos.model.Inscricao;
import com.faculdade.eventos.model.StatusInscricao;

import java.time.OffsetDateTime;

// Inscrição devolvida pela API, com os dados do evento e do participante.
public record InscricaoResponse(
        Long id,
        EventoResumoResponse evento,
        ParticipanteResponse participante,
        StatusInscricao status,
        OffsetDateTime dataInscricao,
        OffsetDateTime atualizadoEm
) {

    public static InscricaoResponse de(Inscricao inscricao) {
        return new InscricaoResponse(
                inscricao.getId(),
                EventoResumoResponse.de(inscricao.getEvento()),
                ParticipanteResponse.de(inscricao.getParticipante()),
                inscricao.getStatus(),
                inscricao.getDataInscricao(),
                inscricao.getAtualizadoEm()
        );
    }
}
