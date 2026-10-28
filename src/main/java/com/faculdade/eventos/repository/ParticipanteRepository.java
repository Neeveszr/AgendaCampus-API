package com.faculdade.eventos.repository;

import com.faculdade.eventos.model.Participante;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipanteRepository
        extends JpaRepository<Participante, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // O e-mail já é gravado em minúsculas, então só o nome precisa de LOWER.
    @Query("""
            SELECT p FROM Participante p
            WHERE LOWER(p.nome) LIKE :nome ESCAPE '!'
              AND p.email LIKE :email ESCAPE '!'
            """)
    Page<Participante> filtrar(
            @Param("nome") String nome,
            @Param("email") String email,
            Pageable pageable
    );
}
