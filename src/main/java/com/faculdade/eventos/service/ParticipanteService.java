package com.faculdade.eventos.service;

import com.faculdade.eventos.exception.ConflitoException;
import com.faculdade.eventos.exception.RecursoNaoEncontradoException;
import com.faculdade.eventos.model.Participante;
import com.faculdade.eventos.repository.InscricaoRepository;
import com.faculdade.eventos.repository.ParticipanteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;
    private final InscricaoRepository inscricaoRepository;

    public ParticipanteService(
            ParticipanteRepository participanteRepository,
            InscricaoRepository inscricaoRepository
    ) {
        this.participanteRepository = participanteRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    public Page<Participante> listar(
            String nome,
            String email,
            Pageable pageable
    ) {
        return participanteRepository.filtrar(
                PadraoBusca.contendo(nome), PadraoBusca.contendo(email), pageable
        );
    }

    public Participante buscarPorId(Long id) {
        return buscarEntidade(id);
    }

    @Transactional
    public Participante cadastrar(Participante dados) {
        String email = dados.getEmail();

        if (participanteRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe um participante com esse e-mail");
        }

        // Cria um participante novo só com nome e e-mail (id e data de criação são ignorados).
        Participante participante = new Participante(
                dados.getNome().strip(),
                email
        );

        return participanteRepository.saveAndFlush(participante);
    }

    @Transactional
    public Participante atualizar(
            Long id,
            Participante dados
    ) {
        Participante participante = buscarEntidade(id);
        String email = dados.getEmail();

        if (participanteRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflitoException("Já existe um participante com esse e-mail");
        }

        participante.setNome(dados.getNome().strip());
        participante.setEmail(email);

        return participanteRepository.saveAndFlush(participante);
    }

    @Transactional
    public void excluir(Long id) {
        Participante participante = buscarEntidade(id);

        if (inscricaoRepository.existsByParticipanteId(id)) {
            throw new ConflitoException(
                    "Não é possível excluir um participante com inscrições vinculadas"
            );
        }

        participanteRepository.delete(participante);
        participanteRepository.flush();
    }

    private Participante buscarEntidade(Long id) {
        return participanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Participante não encontrado com ID: " + id
                ));
    }
}
