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
            OffsetDateTime inicioDe,
            OffsetDateTime inicioAte,
            boolean somenteDisponiveis,
            Pageable pageable
    ) {
        if (inicioDe != null && inicioAte != null
                && inicioDe.isAfter(inicioAte)) {
            throw new RequisicaoInvalidaException(
                    "O início do período não pode ser posterior ao fim"
            );
        }

        Page<Evento> eventos = eventoRepository.filtrar(
                PadraoBusca.contendo(nome), PadraoBusca.contendo(local), inicioDe, inicioAte,
                somenteDisponiveis, OffsetDateTime.now(), StatusInscricao.CONFIRMADA,
                pageable
        );

        return PaginaResponse.de(eventos.map(this::paraResposta));
    }

    public EventoResponse buscarPorId(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> eventoNaoEncontrado(id));
        return paraResposta(evento);
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
        Evento evento = buscarComBloqueio(id);
        OffsetDateTime agora = OffsetDateTime.now();

        validarPrazo(dados);

        boolean alterouDatas =
                !evento.getDataInicio().isEqual(dados.dataInicio())
                        || !evento.getPrazoInscricao()
                        .isEqual(dados.prazoInscricao());

        if (!evento.getDataInicio().isAfter(agora) && alterouDatas) {
            throw new ConflitoException(
                    "As datas não podem ser alteradas após o início do evento"
            );
        }

        if (evento.getDataInicio().isAfter(agora)
                && !dados.dataInicio().isAfter(agora)) {
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
        Evento evento = buscarComBloqueio(id);

        // Usa o @OneToMany: qualquer inscrição, mesmo cancelada, impede a exclusão.
        if (!evento.getInscricoes().isEmpty()) {
            throw new ConflitoException(
                    "Não é possível excluir um evento com inscrições vinculadas"
            );
        }

        eventoRepository.delete(evento);
        eventoRepository.flush();
    }

    // Trava o evento até o fim da transação, para que alterações simultâneas não se sobreponham.
    private Evento buscarComBloqueio(Long id) {
        return eventoRepository.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> eventoNaoEncontrado(id));
    }

    private RecursoNaoEncontradoException eventoNaoEncontrado(Long id) {
        return new RecursoNaoEncontradoException("Evento não encontrado com ID: " + id);
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
