import { requisicao, mensagem, executar, dataFormatada } from "./comum.js";
import { elemento, botao, linhaVazia, paginacao, parametros } from "./interface.js";

const tabela = document.querySelector("#participantes");
const filtrosForm = document.querySelector("#filtros");
const form = document.querySelector("#form-participante");
let filtros = new URLSearchParams({ sort: "nome,asc" });
let paginaAtual = 0;
let editando = null;
let versao = 0;
let versaoEdicao = 0;

function limparFormulario() {
    versaoEdicao++;
    editando = null;
    form.reset();
    document.querySelector("#titulo-form").textContent = "Novo participante";
}

async function editar(id) {
    const atual = ++versaoEdicao;
    try {
        const participante = await requisicao("/participantes/" + id);
        // Novo, Cancelar ou outra edição tornam esta resposta desatualizada.
        if (atual !== versaoEdicao) return;
        editando = id;
        form.elements.nome.value = participante.nome;
        form.elements.email.value = participante.email;
        document.querySelector("#titulo-form").textContent = "Editar participante";
        form.elements.nome.focus();
    } catch (erro) {
        if (atual === versaoEdicao) throw erro;
    }
}

function linha(participante) {
    const tr = elemento("tr");
    const acoes = elemento("div", "actions");
    acoes.append(
        botao("Editar", "btn secondary", b => executar(b, () => editar(participante.id))),
        botao("Excluir", "btn danger", b => {
            if (!confirm('Excluir o participante "' + participante.nome + '"? Esta ação não pode ser desfeita.')) return;
            executar(b, async () => {
                await requisicao("/participantes/" + participante.id, "DELETE");
                if (editando === participante.id) limparFormulario();
                mensagem("Participante excluído com sucesso.");
                await carregar(paginaAtual);
            });
        })
    );
    const celula = elemento("td");
    celula.append(acoes);
    tr.append(elemento("td", "", participante.nome), elemento("td", "", participante.email),
        elemento("td", "", dataFormatada(participante.criadoEm, false)), celula);
    return tr;
}

async function carregar(pagina = 0) {
    const atual = ++versao;
    linhaVazia(tabela, 4, "Carregando participantes…");
    document.querySelector("#paginacao").replaceChildren();
    try {
        const busca = new URLSearchParams(filtros);
        busca.set("page", pagina);
        busca.set("size", 10);
        const resultado = await requisicao("/participantes?" + busca);
        if (atual !== versao) return;
        if (resultado.empty && pagina > 0) return carregar(pagina - 1);
        paginaAtual = pagina;
        tabela.replaceChildren(...resultado.content.map(linha));
        if (resultado.empty) linhaVazia(tabela, 4, "Nenhum participante encontrado. Cadastre uma nova conexão.");
        paginacao(resultado, carregar);
    } catch (erro) {
        if (atual === versao) {
            linhaVazia(tabela, 4, "Não foi possível carregar. Use Filtrar para tentar novamente.");
            mensagem(erro.message, true);
        }
    }
}

form.elements.email.addEventListener("input", () => {
    // Aceita espaços nas extremidades antes da validação HTML do e-mail.
    form.elements.email.value = form.elements.email.value.trim();
});
form.addEventListener("submit", evento => {
    evento.preventDefault();
    executar(form.querySelector("button[type=submit]"), async () => {
        await requisicao("/participantes" + (editando ? "/" + editando : ""), editando ? "PUT" : "POST", {
            nome: form.elements.nome.value.trim(), email: form.elements.email.value.trim()
        });
        mensagem(editando ? "Participante atualizado com sucesso." : "Participante cadastrado com sucesso.");
        limparFormulario();
        await carregar();
    });
});
document.querySelector("#novo").addEventListener("click", () => { limparFormulario(); form.elements.nome.focus(); });
document.querySelector("#cancelar").addEventListener("click", limparFormulario);
filtrosForm.addEventListener("submit", evento => {
    evento.preventDefault();
    filtros = parametros(filtrosForm);
    mensagem("");
    carregar();
});
filtrosForm.addEventListener("reset", () => {
    filtros = new URLSearchParams({ sort: "nome,asc" });
    mensagem("");
    carregar();
});
carregar();

