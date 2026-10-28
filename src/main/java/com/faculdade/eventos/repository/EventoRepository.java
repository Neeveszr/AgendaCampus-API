package com.faculdade.eventos.repository;

import com.faculdade.eventos.exception.RecursoNaoEncontradoException;
import com.faculdade.eventos.model.Evento;
import com.faculdade.eventos.model.StatusInscricao;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface EventoRepository
        extends JpaRepository<Evento, Long> {
    // COALESCE usa a própria coluna quando o limite não foi informado.
    @Query("""
            SELECT e FROM Evento e
            WHERE LOWER(e.nome) LIKE :nome ESCAPE '!'
              AND LOWER(e.local) LIKE :local ESCAPE '!'
              AND e.dataInicio >= COALESCE(:inicioDe, e.dataInicio)
              AND e.dataInicio <= COALESCE(:inicioAte, e.dataInicio)
              AND (:somenteDisponiveis = false OR (
                  e.prazoInscricao > :agora AND e.dataInicio > :agora
                  AND e.capacidade > (
                      SELECT COUNT(i) FROM Inscricao i
                      WHERE i.evento = e AND i.status = :status
                  )
              ))
            """)
    Page<Evento> filtrar(
            @Param("nome") String nome,
            @Param("local") String local,
            @Param("inicioDe") OffsetDateTime inicioDe,
            @Param("inicioAte") OffsetDateTime inicioAte,
            @Param("somenteDisponiveis") boolean somenteDisponiveis,
            @Param("agora") OffsetDateTime agora,
            @Param("status") StatusInscricao status,
            Pageable pageable
    );

    // Serializa operações que alteram a disponibilidade de vagas do mesmo evento.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> buscarPorIdComBloqueio(@Param("id") Long id);

    default Evento obterComBloqueio(Long id) {
        return buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Evento não encontrado com ID: " + id
                ));
    }
}
