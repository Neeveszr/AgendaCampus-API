package com.faculdade.eventos.repository;

import com.faculdade.eventos.model.Inscricao;
import com.faculdade.eventos.model.StatusInscricao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InscricaoRepository
        extends JpaRepository<Inscricao, Long> {

    // Carrega evento e participante no mesmo SELECT, evitando consultas por linha.
    @EntityGraph(attributePaths = {"evento", "participante"})
    @Query("""
            SELECT i FROM Inscricao i
            WHERE i.evento.id = COALESCE(:eventoId, i.evento.id)
              AND i.participante.id = COALESCE(:participanteId, i.participante.id)
              AND i.status = COALESCE(:status, i.status)
            """)
    Page<Inscricao> filtrar(
            @Param("eventoId") Long eventoId,
            @Param("participanteId") Long participanteId,
            @Param("status") StatusInscricao status,
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

    @Query("SELECT i.evento.id FROM Inscricao i WHERE i.id = :id")
    Optional<Long> buscarEventoIdPorInscricaoId(@Param("id") Long id);
}
