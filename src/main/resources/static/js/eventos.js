import { requisicao, mensagem, dataFormatada, inscricoesAbertas, contagem } from "./comum.js";
import { elemento, link, paginacao, parametros } from "./interface.js";

const formulario = document.querySelector("#filtros");
const area = document.querySelector("#eventos");
const resumo = document.querySelector("#resumo");
let filtros = new URLSearchParams({ sort: "dataInicio,asc" });
let versao = 0;

function cartao(evento) {
    const card = elemento("article", "event-card");
    const arte = elemento("div", "card-art");
    arte.setAttribute("aria-hidden", "true");
    // Ilustração fixa; não recebe dados externos.
    arte.innerHTML = '<svg viewBox="0 0 48 48" fill="none"><rect x="8" y="11" width="32" height="30" rx="5" stroke="currentColor" stroke-width="3"/><path d="M16 6v11M32 6v11M8 22h32M17 31l5 5 10-10" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>';
    const corpo = elemento("div", "card-body");
    const titulo = elemento("h3");
    titulo.append(link(evento.nome, "/evento.html?id=" + evento.id));
    const disponibilidade = elemento("div", "card-availability");
    const encerrado = !inscricoesAbertas(evento);
    const texto = encerrado ? "Inscrições encerradas" : evento.vagasDisponiveis > 0
        ? contagem(evento.vagasDisponiveis, "vaga livre", "vagas livres") : "Lotado";
    disponibilidade.append(elemento("span", "badge " + (!encerrado && evento.vagasDisponiveis > 0 ? "green" : ""), texto),
        elemento("span", "count", "Total: " + evento.capacidade));
    const barra = elemento("progress");
    barra.max = evento.capacidade;
    barra.value = evento.capacidade - evento.vagasDisponiveis;
    barra.setAttribute("aria-label", "Vagas ocupadas");
    corpo.append(titulo, elemento("p", "card-meta", "◷ " + dataFormatada(evento.dataInicio)),
        elemento("p", "card-meta", "⌖ " + evento.local), disponibilidade, barra,
        elemento("p", "card-description", evento.descricao || "Conheça os detalhes e participe deste encontro."),
        link("Ver detalhes →", "/evento.html?id=" + evento.id, "btn secondary"));
    card.append(arte, corpo);
    return card;
}

async function carregar(pagina = 0) {
    const atual = ++versao;
    resumo.textContent = "Carregando eventos…";
    document.querySelector("#paginacao").replaceChildren();
    area.replaceChildren();
    try {
        const busca = new URLSearchParams(filtros);
        busca.set("page", pagina);
        busca.set("size", 6);
        const resultado = await requisicao("/eventos?" + busca);
        if (atual !== versao) return;
        resumo.textContent = contagem(resultado.totalElements, "evento", "eventos") + " para descobrir";
        area.replaceChildren(...resultado.content.map(cartao));
        if (resultado.empty) area.append(elemento("div", "panel empty", "Nenhum evento encontrado. Ajuste os filtros ou crie o primeiro encontro."));
        paginacao(resultado, carregar);
    } catch (erro) {
        if (atual === versao) {
            resumo.textContent = "Não foi possível carregar os eventos. Tente aplicar os filtros novamente.";
            mensagem(erro.message, true);
        }
    }
}

formulario.addEventListener("submit", evento => {
    evento.preventDefault();
    const novosFiltros = parametros(formulario);
    if (novosFiltros.has("somenteDisponiveis")) novosFiltros.set("somenteDisponiveis", "true");
    const inicio = novosFiltros.get("inicioDe");
    const fim = novosFiltros.get("inicioAte");
    if (inicio && fim && inicio > fim) return mensagem("A data inicial do filtro não pode ser posterior à final.", true);
    if (inicio) novosFiltros.set("inicioDe", new Date(inicio + "T00:00:00").toISOString());
    if (fim) novosFiltros.set("inicioAte", new Date(fim + "T23:59:59.999").toISOString());
    filtros = novosFiltros;
    mensagem("");
    carregar();
});
formulario.addEventListener("reset", () => {
    filtros = new URLSearchParams({ sort: "dataInicio,asc" });
    mensagem("");
    carregar();
});
carregar();

