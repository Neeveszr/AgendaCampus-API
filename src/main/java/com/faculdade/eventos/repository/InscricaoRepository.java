package com.faculdade.eventos.repository;

import com.faculdade.eventos.model.Inscricao;
import com.faculdade.eventos.model.StatusInscricao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscricaoRepository
        extends JpaRepository<Inscricao, Long> {

    // Listagens: o @EntityGraph traz evento e participante no mesmo SELECT,
    // em vez de uma consulta extra para cada inscrição da página.
    @Override
    @EntityGraph(attributePaths = {"evento", "participante"})
    Page<Inscricao> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"evento", "participante"})
    Page<Inscricao> findByEventoId(Long eventoId, Pageable pageable);

    @EntityGraph(attributePaths = {"evento", "participante"})
    Page<Inscricao> findByStatus(StatusInscricao status, Pageable pageable);

    @EntityGraph(attributePaths = {"evento", "participante"})
    Page<Inscricao> findByEventoIdAndStatus(
            Long eventoId,
            StatusInscricao status,
            Pageable pageable
    );

    boolean existsByEventoIdAndParticipanteId(
            Long eventoId,
            Long participanteId
    );

    long countByEventoIdAndStatus(
            Long eventoId,
            StatusInscricao status
    );
}
