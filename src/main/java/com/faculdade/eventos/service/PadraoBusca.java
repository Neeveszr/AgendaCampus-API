package com.faculdade.eventos.service;

import java.util.Locale;

final class PadraoBusca {
    private PadraoBusca() {
    }

    // Monta o padrão de um LIKE ... ESCAPE '!' para buscar o texto informado literalmente.
    static String contendo(String valor) {
        if (valor == null || valor.isBlank()) {
            return "%";
        }
        String texto = valor.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + texto + "%";
    }
}
