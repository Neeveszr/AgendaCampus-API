package com.faculdade.eventos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

// Dados recebidos no cadastro e na atualização de um participante (POST e PUT /participantes).
public record ParticipanteRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email
) {

    // Roda antes da validação: o e-mail chega sem espaços nas extremidades e em minúsculas.
    public ParticipanteRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
