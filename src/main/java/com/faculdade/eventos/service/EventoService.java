package com.faculdade.eventos.service;

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

    public Page<Evento> listar(
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

        eventos.forEach(this::calcularVagas);

        return eventos;
    }

    public Evento buscarPorId(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Evento não encontrado com ID: " + id
                ));
        calcularVagas(evento);
        return evento;
    }

    @Transactional
    public Evento cadastrar(Evento dados) {
        validarPrazo(dados);

        if (!dados.getDataInicio().isAfter(OffsetDateTime.now())) {
            throw new RequisicaoInvalidaException(
                    "Um novo evento deve ter data de início futura"
            );
        }

        // Cria um evento novo só com os campos permitidos (id e datas de controle são ignorados).
        Evento evento = new Evento(
                dados.getNome().strip(),
                descricaoTratada(dados.getDescricao()),
                dados.getLocal().strip(),
                dados.getDataInicio(),
                dados.getPrazoInscricao(),
                dados.getCapacidade()
        );

        Evento salvo = eventoRepository.saveAndFlush(evento);
        salvo.setVagasDisponiveis(salvo.getCapacidade());

        return salvo;
    }

    @Transactional
    public Evento atualizar(Long id, Evento dados) {
        Evento evento = eventoRepository.obterComBloqueio(id);
        OffsetDateTime agora = OffsetDateTime.now();

        validarPrazo(dados);

        boolean alterouDatas =
                !evento.getDataInicio().isEqual(dados.getDataInicio())
                        || !evento.getPrazoInscricao()
                        .isEqual(dados.getPrazoInscricao());

        if (!evento.getDataInicio().isAfter(agora) && alterouDatas) {
            throw new ConflitoException(
                    "As datas não podem ser alteradas após o início do evento"
            );
        }

        if (evento.getDataInicio().isAfter(agora)
                && !dados.getDataInicio().isAfter(agora)) {
            throw new RequisicaoInvalidaException(
                    "A nova data de início deve ser futura"
            );
        }

        long confirmadas = contarConfirmadas(id);

        if (dados.getCapacidade() < confirmadas) {
            throw new ConflitoException(
                    "A capacidade não pode ser menor que a quantidade "
                            + "de inscrições confirmadas: " + confirmadas
            );
        }

        evento.setNome(dados.getNome().strip());
        evento.setDescricao(descricaoTratada(dados.getDescricao()));
        evento.setLocal(dados.getLocal().strip());
        evento.setDataInicio(dados.getDataInicio());
        evento.setPrazoInscricao(dados.getPrazoInscricao());
        evento.setCapacidade(dados.getCapacidade());

        Evento salvo = eventoRepository.saveAndFlush(evento);
        salvo.setVagasDisponiveis(salvo.getCapacidade() - confirmadas);

        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        Evento evento = eventoRepository.obterComBloqueio(id);

        if (inscricaoRepository.existsByEventoId(id)) {
            throw new ConflitoException(
                    "Não é possível excluir um evento com inscrições vinculadas"
            );
        }

        eventoRepository.delete(evento);
        eventoRepository.flush();
    }

    // A descrição é opcional: texto em branco é gravado como ausente.
    private String descricaoTratada(String descricao) {
        return descricao == null || descricao.isBlank() ? null : descricao.strip();
    }

    private void validarPrazo(Evento dados) {
        if (dados.getPrazoInscricao().isAfter(dados.getDataInicio())) {
            throw new RequisicaoInvalidaException(
                    "O prazo de inscrição não pode ser posterior "
                            + "ao início do evento"
            );
        }
    }

    // Vagas disponíveis = capacidade - inscrições confirmadas.
    private void calcularVagas(Evento evento) {
        evento.setVagasDisponiveis(evento.getCapacidade() - contarConfirmadas(evento.getId()));
    }

    private long contarConfirmadas(Long eventoId) {
        return inscricaoRepository.countByEventoIdAndStatus(
                eventoId,
                StatusInscricao.CONFIRMADA
        );
    }
}
