import { requisicao, mensagem, executar, dataFormatada, avisarDepois, idDaPagina, inscricoesAbertas, contagem } from "./comum.js";
import { elemento, linhaVazia, paginacao, statusInscricao } from "./interface.js";
import { criarSeletor } from "./seletor.js";

const form = document.querySelector("#inscrever");
const participante = criarSeletor("participante", "participantes", true);
const tabela = document.querySelector("#inscritos");
let id;
let eventoAtual;
let versaoInscritos = 0;

async function carregarEvento() {
    eventoAtual = await requisicao("/eventos/" + id);
    const evento = eventoAtual;
    document.querySelector("#nome").textContent = evento.nome;
    document.title = evento.nome + " · Agenda Campus";
    document.querySelector("#descricao").textContent = evento.descricao || "Este evento ainda não possui uma descrição.";
    document.querySelector("#local").textContent = evento.local;
    document.querySelector("#inicio").textContent = dataFormatada(evento.dataInicio);
    document.querySelector("#prazo").textContent = dataFormatada(evento.prazoInscricao);
    document.querySelector("#vagas").textContent = contagem(evento.vagasDisponiveis, "vaga disponível", "vagas disponíveis");
    const confirmadas = evento.capacidade - evento.vagasDisponiveis;
    document.querySelector("#capacidade").textContent = contagem(confirmadas, "inscrição confirmada", "inscrições confirmadas")
        + " · Capacidade: " + evento.capacidade;
    const barra = document.querySelector("#ocupacao");
    barra.max = evento.capacidade;
    barra.value = confirmadas;
    const encerrado = !inscricoesAbertas(evento);
    document.querySelector("#situacao").textContent = encerrado ? "As inscrições para este evento estão encerradas."
        : evento.vagasDisponiveis === 0 ? "Todas as vagas foram preenchidas." : "Inscrições abertas. Escolha um participante abaixo.";
    // A API confirma a disponibilidade novamente ao enviar, inclusive em concorrência.
    form.hidden = encerrado || evento.vagasDisponiveis === 0;
    document.querySelector("#editar").href = "/evento-form.html?id=" + id;
    document.querySelector("#gerenciar").href = "/inscricoes.html?eventoId=" + id;
    document.querySelector("#detalhes").hidden = false;
}

async function carregarInscritos(pagina = 0) {
    const atual = ++versaoInscritos;
    linhaVazia(tabela, 3, "Carregando inscrições…");
    document.querySelector("#paginacao").replaceChildren();
    try {
        const resultado = await requisicao("/inscricoes?" + new URLSearchParams({ eventoId: id, status: "CONFIRMADA", page: pagina, size: 10, sort: "id,asc" }));
        if (atual !== versaoInscritos) return;
        tabela.replaceChildren(...resultado.content.map(item => {
            const tr = elemento("tr");
            const status = elemento("td");
            status.append(statusInscricao(item.status));
            tr.append(elemento("td", "", item.participante.nome), elemento("td", "", dataFormatada(item.dataInscricao)), status);
            return tr;
        }));
        if (resultado.empty) linhaVazia(tabela, 3, "Ainda não há inscrições confirmadas.");
        paginacao(resultado, carregarInscritos);
    } catch (erro) {
        if (atual === versaoInscritos) {
            linhaVazia(tabela, 3, "Não foi possível carregar as inscrições. Recarregue a página para tentar novamente.");
            mensagem(erro.message, true);
        }
    }
}

form.addEventListener("submit", e => {
    e.preventDefault();
    executar(form.querySelector("button[type=submit]"), async () => {
        if (!participante.select.value) throw new Error("Selecione um participante.");
        await requisicao("/inscricoes", "POST", { evento: { id: Number(id) }, participante: { id: Number(participante.select.value) } });
        mensagem("Inscrição confirmada com sucesso.");
        participante.select.value = "";
        await carregarEvento();
        await carregarInscritos();
    });
});
document.querySelector("#excluir").addEventListener("click", e => {
    if (!confirm('Excluir o evento "' + eventoAtual.nome + '"? Esta ação não pode ser desfeita.')) return;
    executar(e.currentTarget, async () => {
        await requisicao("/eventos/" + id, "DELETE");
        avisarDepois("Evento excluído com sucesso.");
        location.href = "/";
    });
});
async function iniciar() {
    try {
        id = idDaPagina();
        await carregarEvento();
        await carregarInscritos();
    } catch (erro) {
        document.querySelector("#nome").textContent = "Não foi possível abrir o evento";
        mensagem(erro.message, true);
    }
}
iniciar();

