package com.faculdade.eventos.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Página de resultados devolvida pelas listagens, sem expor o objeto Page do Spring.
public record PaginaResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast(),
                pagina.isEmpty()
        );
    }
}
