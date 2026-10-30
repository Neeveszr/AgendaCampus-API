import { requisicao, mensagem, executar, dataLocal, avisarDepois, idDaPagina } from "./comum.js";

const form = document.querySelector("#form-evento");
const campos = form.elements;
const editando = new URLSearchParams(location.search).has("id");
let id;
let original;
let inicioExibido;
let prazoExibido;

async function iniciar() {
    if (!editando) return;
    document.querySelector("#campos-evento").disabled = true;
    try {
        id = idDaPagina();
        original = await requisicao("/eventos/" + id);
        document.querySelector("h1").textContent = "Novas ideias, mesmo encontro";
        document.querySelector("#titulo-form").textContent = "Editar evento";
        document.title = "Editar evento · Agenda Campus";
        document.querySelector("#cancelar").href = "/evento.html?id=" + id;
        for (const nome of ["nome", "local", "descricao", "capacidade"]) campos[nome].value = original[nome] ?? "";
        inicioExibido = dataLocal(original.dataInicio);
        prazoExibido = dataLocal(original.prazoInscricao);
        campos.dataInicio.value = inicioExibido;
        campos.prazoInscricao.value = prazoExibido;
        // O navegador pode normalizar segundos :00 ao atribuir o valor.
        inicioExibido = campos.dataInicio.value;
        prazoExibido = campos.prazoInscricao.value;
        document.querySelector("#campos-evento").disabled = false;
    } catch (erro) {
        mensagem(erro.message, true);
    }
}

form.addEventListener("submit", evento => {
    evento.preventDefault();
    executar(form.querySelector("button[type=submit]"), async () => {
        const dados = {
            nome: campos.nome.value.trim(), local: campos.local.value.trim(),
            descricao: campos.descricao.value.trim(), capacidade: Number(campos.capacidade.value),
            dataInicio: original && campos.dataInicio.value === inicioExibido ? original.dataInicio : new Date(campos.dataInicio.value).toISOString(),
            prazoInscricao: original && campos.prazoInscricao.value === prazoExibido ? original.prazoInscricao : new Date(campos.prazoInscricao.value).toISOString()
        };
        if (new Date(dados.prazoInscricao) > new Date(dados.dataInicio)) throw new Error("O prazo de inscrição não pode ultrapassar o início do evento.");
        const salvo = await requisicao("/eventos" + (editando ? "/" + id : ""), editando ? "PUT" : "POST", dados);
        avisarDepois(editando ? "Evento atualizado com sucesso." : "Evento criado com sucesso.");
        location.href = "/evento.html?id=" + salvo.id;
    });
});
iniciar();

