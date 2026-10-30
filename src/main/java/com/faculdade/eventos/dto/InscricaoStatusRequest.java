package com.faculdade.eventos.dto;

import com.faculdade.eventos.model.StatusInscricao;

import jakarta.validation.constraints.NotNull;

// Novo status de uma inscrição: CANCELADA para cancelar, CONFIRMADA para reativar (PUT /inscricoes/{id}).
public record InscricaoStatusRequest(
        @NotNull(message = "O status é obrigatório")
        StatusInscricao status
) {
}
