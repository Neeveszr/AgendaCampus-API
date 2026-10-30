package com.faculdade.eventos.repository;

import com.faculdade.eventos.model.Participante;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipanteRepository
        extends JpaRepository<Participante, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // Busca por parte do nome e do e-mail; texto vazio aceita qualquer valor.
    Page<Participante> findByNomeContainingIgnoreCaseAndEmailContainingIgnoreCase(
            String nome,
            String email,
            Pageable pageable
    );
}
