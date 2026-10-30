package com.faculdade.eventos.service;

import com.faculdade.eventos.dto.InscricaoRequest;
import com.faculdade.eventos.dto.InscricaoResponse;
import com.faculdade.eventos.dto.PaginaResponse;
import com.faculdade.eventos.exception.ConflitoException;
import com.faculdade.eventos.exception.RecursoNaoEncontradoException;
import com.faculdade.eventos.model.Evento;
import com.faculdade.eventos.model.Inscricao;
import com.faculdade.eventos.model.Participante;
import com.faculdade.eventos.model.StatusInscricao;
import com.faculdade.eventos.repository.EventoRepository;
import com.faculdade.eventos.repository.InscricaoRepository;
import com.faculdade.eventos.repository.ParticipanteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional(readOnly = true)
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;
    private final ParticipanteRepository participanteRepository;

    public InscricaoService(
            InscricaoRepository inscricaoRepository,
            EventoRepository eventoRepository,
            ParticipanteRepository participanteRepository
    ) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
        this.participanteRepository = participanteRepository;
    }

    public PaginaResponse<InscricaoResponse> listar(
            Long eventoId,
            StatusInscricao status,
            Pageable pageable
    ) {
        // Escolhe o método do repository conforme os filtros informados.
        Page<Inscricao> inscricoes;
        if (eventoId != null && status != null) {
            inscricoes = inscricaoRepository.findByEventoIdAndStatus(eventoId, status, pageable);
        } else if (eventoId != null) {
            inscricoes = inscricaoRepository.findByEventoId(eventoId, pageable);
        } else if (status != null) {
            inscricoes = inscricaoRepository.findByStatus(status, pageable);
        } else {
            inscricoes = inscricaoRepository.findAll(pageable);
        }

        return PaginaResponse.de(inscricoes.map(InscricaoResponse::de));
    }

    public InscricaoResponse buscarPorId(Long id) {
        return InscricaoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public InscricaoResponse cadastrar(InscricaoRequest dados) {
        // Trava o evento: inscrições simultâneas esperam e recontam as vagas.
        Evento evento = eventoRepository.findComBloqueioById(dados.eventoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Evento não encontrado com ID: " + dados.eventoId()
                ));

        Participante participante = participanteRepository
                .findById(dados.participanteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Participante não encontrado com ID: " + dados.participanteId()
                ));

        if (inscricaoRepository.existsByEventoIdAndParticipanteId(
                evento.getId(),
                participante.getId()
        )) {
            throw new ConflitoException(
                    "O participante já possui uma inscrição neste evento. "
                            + "Se estiver cancelada, reative a inscrição existente."
            );
        }

        validarInscricaoAberta(evento);
        validarVagaDisponivel(evento);

        Inscricao inscricao = new Inscricao(evento, participante);

        return InscricaoResponse.de(inscricaoRepository.saveAndFlush(inscricao));
    }

    @Transactional
    public InscricaoResponse atualizarStatus(Long id, StatusInscricao novoStatus) {
        Inscricao inscricao = buscarEntidade(id);
        Evento evento = inscricao.getEvento();

        if (inscricao.getStatus() == novoStatus) {
            return InscricaoResponse.de(inscricao);
        }

        if (novoStatus == StatusInscricao.CONFIRMADA) {
            // Reativar segue as mesmas regras de uma inscrição nova, com o evento travado.
            eventoRepository.findComBloqueioById(evento.getId());
            validarInscricaoAberta(evento);
            validarVagaDisponivel(evento);
        } else if (!evento.getDataInicio().isAfter(OffsetDateTime.now())) {
            throw new ConflitoException(
                    "Não é possível cancelar após o início do evento"
            );
        }

        inscricao.setStatus(novoStatus);

        return InscricaoResponse.de(inscricaoRepository.saveAndFlush(inscricao));
    }

    @Transactional
    public void excluir(Long id) {
        Inscricao inscricao = buscarEntidade(id);

        if (inscricao.getStatus() != StatusInscricao.CANCELADA) {
            throw new ConflitoException(
                    "Somente inscrições canceladas podem ser excluídas"
            );
        }

        inscricaoRepository.delete(inscricao);
        inscricaoRepository.flush();
    }

    private Inscricao buscarEntidade(Long id) {
        return inscricaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Inscrição não encontrada com ID: " + id
                ));
    }

    private void validarInscricaoAberta(Evento evento) {
        OffsetDateTime agora = OffsetDateTime.now();

        if (!evento.getDataInicio().isAfter(agora)) {
            throw new ConflitoException("O evento já foi iniciado");
        }

        if (!evento.getPrazoInscricao().isAfter(agora)) {
            throw new ConflitoException(
                    "O prazo de inscrição está encerrado"
            );
        }
    }

    private void validarVagaDisponivel(Evento evento) {
        long confirmadas = inscricaoRepository.countByEventoIdAndStatus(
                evento.getId(),
                StatusInscricao.CONFIRMADA
        );

        if (confirmadas >= evento.getCapacidade()) {
            throw new ConflitoException(
                    "O evento não possui vagas disponíveis"
            );
        }
    }
}
