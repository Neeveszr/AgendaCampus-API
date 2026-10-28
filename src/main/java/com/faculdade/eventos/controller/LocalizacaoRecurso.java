package com.faculdade.eventos.controller;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

final class LocalizacaoRecurso {
    private LocalizacaoRecurso() {
    }

    // URL do recurso recém-criado, usada no cabeçalho Location das respostas 201.
    static URI doId(Long id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
