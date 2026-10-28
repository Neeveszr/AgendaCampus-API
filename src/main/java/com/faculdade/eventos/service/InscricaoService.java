package com.faculdade.eventos.service;

import com.faculdade.eventos.exception.ConflitoException;
import com.faculdade.eventos.exception.RecursoNaoEncontradoException;
import com.faculdade.eventos.exception.RequisicaoInvalidaException;
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

    public Page<Inscricao> listar(
            Long eventoId,
            Long participanteId,
            StatusInscricao status,
            Pageable pageable
    ) {
        return inscricaoRepository.filtrar(eventoId, participanteId, status, pageable);
    }

    public Inscricao buscarPorId(Long id) {
        return buscarEntidade(id);
    }

    @Transactional
    public Inscricao cadastrar(Inscricao dados) {
        // O corpo traz apenas os IDs: { "evento": { "id": 1 }, "participante": { "id": 3 } }
        Long eventoId = dados.getEvento() == null ? null : dados.getEvento().getId();
        Long participanteId = dados.getParticipante() == null ? null : dados.getParticipante().getId();

        if (eventoId == null || participanteId == null) {
            throw new RequisicaoInvalidaException(
                    "Informe o evento e o participante da inscrição"
            );
        }

        Evento evento = eventoRepository.obterComBloqueio(eventoId);

        Participante participante = participanteRepository
                .findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Participante não encontrado com ID: " + participanteId
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

        // Cria uma inscrição nova: toda inscrição começa confirmada (o status enviado é ignorado).
        Inscricao inscricao = new Inscricao();
        inscricao.setEvento(evento);
        inscricao.setParticipante(participante);
        inscricao.setStatus(StatusInscricao.CONFIRMADA);

        return inscricaoRepository.saveAndFlush(inscricao);
    }

    @Transactional
    public Inscricao atualizarStatus(Long id, StatusInscricao novoStatus) {
        Inscricao inscricao = buscarInscricaoParaAlteracao(id);
        Evento evento = inscricao.getEvento();

        if (inscricao.getStatus() == novoStatus) {
            return inscricao;
        }

        if (novoStatus == StatusInscricao.CONFIRMADA) {
            validarInscricaoAberta(evento);
            validarVagaDisponivel(evento);
        } else {
            validarEventoNaoIniciado(evento);
        }

        inscricao.setStatus(novoStatus);

        return inscricaoRepository.saveAndFlush(inscricao);
    }

    @Transactional
    public void excluir(Long id) {
        Inscricao inscricao = buscarInscricaoParaAlteracao(id);

        validarEventoNaoIniciado(inscricao.getEvento());

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

    private Inscricao buscarInscricaoParaAlteracao(Long id) {
        Long eventoId = inscricaoRepository.buscarEventoIdPorInscricaoId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Inscrição não encontrada com ID: " + id
                ));

        // O evento deve ser bloqueado ANTES de ler o status da inscrição.
        eventoRepository.obterComBloqueio(eventoId);

        return buscarEntidade(id);
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

    private void validarEventoNaoIniciado(Evento evento) {
        if (!evento.getDataInicio().isAfter(OffsetDateTime.now())) {
            throw new ConflitoException(
                    "A operação não é permitida após o início do evento"
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
