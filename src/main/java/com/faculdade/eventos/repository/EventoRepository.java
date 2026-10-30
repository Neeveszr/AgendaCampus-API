package com.faculdade.eventos.repository;

import com.faculdade.eventos.model.Evento;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface EventoRepository
        extends JpaRepository<Evento, Long> {

    // Busca por parte do nome e do local; texto vazio aceita qualquer valor.
    Page<Evento> findByNomeContainingIgnoreCaseAndLocalContainingIgnoreCase(
            String nome,
            String local,
            Pageable pageable
    );

    // Igual ao findById, mas trava o evento até o fim da transação (SELECT ... FOR UPDATE).
    // Duas inscrições ao mesmo tempo esperam uma pela outra e não passam da capacidade.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Evento> findComBloqueioById(Long id);
}
