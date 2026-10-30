package com.faculdade.eventos.service;

import com.faculdade.eventos.dto.PaginaResponse;
import com.faculdade.eventos.dto.ParticipanteRequest;
import com.faculdade.eventos.dto.ParticipanteResponse;
import com.faculdade.eventos.exception.ConflitoException;
import com.faculdade.eventos.exception.RecursoNaoEncontradoException;
import com.faculdade.eventos.model.Participante;
import com.faculdade.eventos.repository.ParticipanteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;

    public ParticipanteService(ParticipanteRepository participanteRepository) {
        this.participanteRepository = participanteRepository;
    }

    public PaginaResponse<ParticipanteResponse> listar(
            String nome,
            String email,
            Pageable pageable
    ) {
        // Filtro não informado chega como texto vazio, que aceita qualquer valor.
        Page<Participante> participantes = participanteRepository
                .findByNomeContainingIgnoreCaseAndEmailContainingIgnoreCase(nome.strip(), email.strip(), pageable);

        return PaginaResponse.de(participantes.map(ParticipanteResponse::de));
    }

    public ParticipanteResponse buscarPorId(Long id) {
        return ParticipanteResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ParticipanteResponse cadastrar(ParticipanteRequest dados) {
        // O e-mail já chega normalizado pelo ParticipanteRequest.
        if (participanteRepository.existsByEmail(dados.email())) {
            throw new ConflitoException("Já existe um participante com esse e-mail");
        }

        Participante participante = new Participante(
                dados.nome().strip(),
                dados.email()
        );

        return ParticipanteResponse.de(participanteRepository.saveAndFlush(participante));
    }

    @Transactional
    public ParticipanteResponse atualizar(
            Long id,
            ParticipanteRequest dados
    ) {
        Participante participante = buscarEntidade(id);

        if (participanteRepository.existsByEmailAndIdNot(dados.email(), id)) {
            throw new ConflitoException("Já existe um participante com esse e-mail");
        }

        participante.setNome(dados.nome().strip());
        participante.setEmail(dados.email());

        return ParticipanteResponse.de(participanteRepository.saveAndFlush(participante));
    }

    @Transactional
    public void excluir(Long id) {
        Participante participante = buscarEntidade(id);

        // Usa o @OneToMany: qualquer inscrição, mesmo cancelada, impede a exclusão.
        if (!participante.getInscricoes().isEmpty()) {
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
