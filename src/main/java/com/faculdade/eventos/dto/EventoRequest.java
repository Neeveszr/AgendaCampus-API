package com.faculdade.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

// Dados recebidos no cadastro e na atualização de um evento (POST e PUT /eventos).
public record EventoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome,

        String descricao,

        @NotBlank(message = "O local é obrigatório")
        @Size(max = 200, message = "O local deve ter no máximo 200 caracteres")
        String local,

        @NotNull(message = "A data de início é obrigatória")
        OffsetDateTime dataInicio,

        @NotNull(message = "O prazo de inscrição é obrigatório")
        OffsetDateTime prazoInscricao,

        @NotNull(message = "A capacidade é obrigatória")
        @Min(value = 1, message = "A capacidade deve ser maior que zero")
        Integer capacidade
) {
}
