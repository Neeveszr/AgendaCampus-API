package com.faculdade.eventos.dto;

import jakarta.validation.constraints.NotNull;

// Dados recebidos para inscrever um participante em um evento (POST /inscricoes).
public record InscricaoRequest(
        @NotNull(message = "O evento é obrigatório")
        Long eventoId,

        @NotNull(message = "O participante é obrigatório")
        Long participanteId
) {
}
