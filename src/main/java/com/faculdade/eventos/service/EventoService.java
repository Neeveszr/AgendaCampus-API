package com.faculdade.eventos.service;

import com.faculdade.eventos.dto.EventoRequest;
import com.faculdade.eventos.dto.EventoResponse;
import com.faculdade.eventos.dto.PaginaResponse;
import com.faculdade.eventos.exception.ConflitoException;
import com.faculdade.eventos.exception.RecursoNaoEncontradoException;
import com.faculdade.eventos.exception.RequisicaoInvalidaException;
import com.faculdade.eventos.model.Evento;
import com.faculdade.eventos.model.StatusInscricao;
import com.faculdade.eventos.repository.EventoRepository;
import com.faculdade.eventos.repository.InscricaoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional(readOnly = true)
public class EventoService {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;

    public EventoService(
            EventoRepository eventoRepository,
            InscricaoRepository inscricaoRepository
    ) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    public PaginaResponse<EventoResponse> listar(
            String nome,
            String local,
            Pageable pageable
    ) {
        // Filtro não informado chega como texto vazio, que aceita qualquer valor.
        Page<Evento> eventos = eventoRepository.findByNomeContainingIgnoreCaseAndLocalContainingIgnoreCase(
                nome.strip(), local.strip(), pageable
        );

        return PaginaResponse.de(eventos.map(this::paraResposta));
    }

    public EventoResponse buscarPorId(Long id) {
        return paraResposta(buscarEntidade(id));
    }

    @Transactional
    public EventoResponse cadastrar(EventoRequest dados) {
        validarPrazo(dados);

        if (!dados.dataInicio().isAfter(OffsetDateTime.now())) {
            throw new RequisicaoInvalidaException(
                    "Um novo evento deve ter data de início futura"
            );
        }

        // Converte o DTO em entidade, já com os textos tratados.
        Evento evento = new Evento(
                dados.nome().strip(),
                descricaoTratada(dados.descricao()),
                dados.local().strip(),
                dados.dataInicio(),
                dados.prazoInscricao(),
                dados.capacidade()
        );

        Evento salvo = eventoRepository.saveAndFlush(evento);

        // Evento novo ainda não tem inscrições: todas as vagas estão livres.
        return EventoResponse.de(salvo, salvo.getCapacidade());
    }

    @Transactional
    public EventoResponse atualizar(Long id, EventoRequest dados) {
        // Trava o evento: uma inscrição simultânea espera, e a contagem abaixo fica correta.
        Evento evento = eventoRepository.findComBloqueioById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Evento não encontrado com ID: " + id
                ));

        validarPrazo(dados);

        // Mudar a data de início só é permitido para uma data futura.
        boolean mudouInicio = !evento.getDataInicio().isEqual(dados.dataInicio());
        if (mudouInicio && !dados.dataInicio().isAfter(OffsetDateTime.now())) {
            throw new RequisicaoInvalidaException(
                    "A nova data de início deve ser futura"
            );
        }

        long confirmadas = contarConfirmadas(id);

        if (dados.capacidade() < confirmadas) {
            throw new ConflitoException(
                    "A capacidade não pode ser menor que a quantidade "
                            + "de inscrições confirmadas: " + confirmadas
            );
        }

        evento.setNome(dados.nome().strip());
        evento.setDescricao(descricaoTratada(dados.descricao()));
        evento.setLocal(dados.local().strip());
        evento.setDataInicio(dados.dataInicio());
        evento.setPrazoInscricao(dados.prazoInscricao());
        evento.setCapacidade(dados.capacidade());

        Evento salvo = eventoRepository.saveAndFlush(evento);

        return EventoResponse.de(salvo, salvo.getCapacidade() - confirmadas);
    }

    @Transactional
    public void excluir(Long id) {
        Evento evento = buscarEntidade(id);

        // Usa o @OneToMany: qualquer inscrição, mesmo cancelada, impede a exclusão.
        if (!evento.getInscricoes().isEmpty()) {
            throw new ConflitoException(
                    "Não é possível excluir um evento com inscrições vinculadas"
            );
        }

        eventoRepository.delete(evento);
        eventoRepository.flush();
    }

    private Evento buscarEntidade(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Evento não encontrado com ID: " + id
                ));
    }

    // A descrição é opcional: texto em branco é gravado como ausente.
    private String descricaoTratada(String descricao) {
        return descricao == null || descricao.isBlank() ? null : descricao.strip();
    }

    private void validarPrazo(EventoRequest dados) {
        if (dados.prazoInscricao().isAfter(dados.dataInicio())) {
            throw new RequisicaoInvalidaException(
                    "O prazo de inscrição não pode ser posterior "
                            + "ao início do evento"
            );
        }
    }

    // Vagas disponíveis = capacidade - inscrições confirmadas.
    private EventoResponse paraResposta(Evento evento) {
        return EventoResponse.de(evento, evento.getCapacidade() - contarConfirmadas(evento.getId()));
    }

    private long contarConfirmadas(Long eventoId) {
        return inscricaoRepository.countByEventoIdAndStatus(
                eventoId,
                StatusInscricao.CONFIRMADA
        );
    }
}
