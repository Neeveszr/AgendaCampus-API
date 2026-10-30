import { requisicao, mensagem, executar, dataFormatada, inscricoesAbertas, contagem, idValido } from "./comum.js";
import { elemento, botao, link, linhaVazia, paginacao, parametros, statusInscricao } from "./interface.js";
import { criarSeletor } from "./seletor.js";

const filtroEvento = criarSeletor("filtro-evento", "eventos");
const novoEvento = criarSeletor("novo-evento", "eventos", true);
const novoParticipante = criarSeletor("novo-participante", "participantes", true);
const filtrosForm = document.querySelector("#filtros");
const form = document.querySelector("#nova-inscricao");
const tabela = document.querySelector("#inscricoes");
let filtros = new URLSearchParams({ sort: "id,desc" });
let paginaAtual = 0;
let versao = 0;
let versaoVagas = 0;

async function consultarVagas() {
    const atual = ++versaoVagas;
    const id = novoEvento.select.value;
    const texto = document.querySelector("#vagas");
    texto.textContent = id ? "Consultando vagas…" : "Selecione um evento para consultar as vagas.";
    if (!id) return;
    try {
        const evento = await requisicao("/eventos/" + id);
        if (atual !== versaoVagas) return;
        const encerrado = !inscricoesAbertas(evento);
        texto.textContent = encerrado ? "Inscrições encerradas para este evento."
            : contagem(evento.vagasDisponiveis, "vaga disponível", "vagas disponíveis") + " neste evento.";
    } catch (erro) {
        if (atual === versaoVagas) {
            texto.textContent = "Não foi possível consultar as vagas.";
            mensagem(erro.message, true);
        }
    }
}
novoEvento.select.addEventListener("change", consultarVagas);

async function alterar(item, status) {
    await requisicao("/inscricoes/" + item.id, "PUT", { status });
    mensagem(status === "CANCELADA" ? "Inscrição cancelada. A vaga foi liberada." : "Inscrição reativada com sucesso.");
    await carregar(paginaAtual);
    await consultarVagas();
}

function linha(item) {
    const tr = elemento("tr");
    const evento = elemento("td");
    evento.append(link(item.evento.nome, "/evento.html?id=" + item.evento.id));
    const status = elemento("td");
    status.append(statusInscricao(item.status));
    const acoes = elemento("div", "actions");
    if (item.status === "CONFIRMADA") {
        acoes.append(botao("Cancelar", "btn secondary", b => {
            if (confirm("Cancelar a inscrição de " + item.participante.nome + "?")) executar(b, () => alterar(item, "CANCELADA"));
        }));
    } else {
        acoes.append(botao("Reativar", "btn secondary", b => executar(b, () => alterar(item, "CONFIRMADA"))),
            botao("Excluir", "btn danger", b => {
                if (!confirm("Excluir definitivamente esta inscrição cancelada?")) return;
                executar(b, async () => {
                    await requisicao("/inscricoes/" + item.id, "DELETE");
                    mensagem("Inscrição excluída com sucesso.");
                    await carregar(paginaAtual);
                });
            }));
    }
    const celula = elemento("td");
    celula.append(acoes);
    tr.append(elemento("td", "", item.participante.nome), evento,
        elemento("td", "", dataFormatada(item.dataInscricao)), status, celula);
    return tr;
}

async function carregar(pagina = 0) {
    const atual = ++versao;
    linhaVazia(tabela, 5, "Carregando inscrições…");
    document.querySelector("#paginacao").replaceChildren();
    try {
        const busca = new URLSearchParams(filtros);
        busca.set("page", pagina);
        busca.set("size", 10);
        const resultado = await requisicao("/inscricoes?" + busca);
        if (atual !== versao) return;
        if (resultado.empty && pagina > 0) return carregar(pagina - 1);
        paginaAtual = pagina;
        tabela.replaceChildren(...resultado.content.map(linha));
        if (resultado.empty) linhaVazia(tabela, 5, "Nenhuma inscrição encontrada.");
        paginacao(resultado, carregar);
    } catch (erro) {
        if (atual === versao) {
            linhaVazia(tabela, 5, "Não foi possível carregar. Aplique os filtros para tentar novamente.");
            mensagem(erro.message, true);
        }
    }
}

form.addEventListener("submit", e => {
    e.preventDefault();
    executar(form.querySelector("button[type=submit]"), async () => {
        if (!novoEvento.select.value || !novoParticipante.select.value) throw new Error("Selecione o evento e o participante.");
        await requisicao("/inscricoes", "POST", {
            eventoId: Number(novoEvento.select.value),
            participanteId: Number(novoParticipante.select.value)
        });
        mensagem("Inscrição confirmada com sucesso.");
        novoParticipante.select.value = "";
        await carregar();
        await consultarVagas();
    });
});
filtrosForm.addEventListener("submit", e => {
    e.preventDefault();
    filtros = parametros(filtrosForm);
    if (filtroEvento.select.value) filtros.set("eventoId", filtroEvento.select.value);
    mensagem("");
    carregar();
});
filtrosForm.addEventListener("reset", () => {
    filtros = new URLSearchParams({ sort: "id,desc" });
    // Aguarda o reset nativo antes de recarregar as opções.
    queueMicrotask(() => { filtroEvento.limpar(); carregar(); });
});
async function iniciar() {
    try {
        const id = new URLSearchParams(location.search).get("eventoId");
        if (id) {
            if (!idValido(id)) throw new Error("O filtro de evento possui um ID inválido.");
            await filtroEvento.pronto;
            await filtroEvento.selecionar(id);
            filtros.set("eventoId", id);
        }
        await carregar();
    } catch (erro) {
        mensagem(erro.message, true);
        linhaVazia(tabela, 5, "Não foi possível aplicar o filtro de evento. Limpe os filtros para continuar.");
    }
}
iniciar();

