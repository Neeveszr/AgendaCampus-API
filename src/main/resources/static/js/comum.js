export async function requisicao(url, metodo = "GET", dados) {
    let resposta;
    try {
        resposta = await fetch(url, {
            method: metodo,
            headers: dados === undefined ? {} : { "Content-Type": "application/json" },
            body: dados === undefined ? undefined : JSON.stringify(dados)
        });
    } catch {
        throw new Error("Não foi possível conectar à aplicação. Verifique sua conexão e tente novamente.");
    }
    if (resposta.status === 204) return null;
    const corpo = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        const campos = Object.entries(corpo?.campos || {}).map(([campo, texto]) => campo + ": " + texto);
        throw new Error([corpo?.detail || "Não foi possível concluir a operação.", ...campos].join("\n"));
    }
    if (corpo === null) throw new Error("A aplicação retornou uma resposta inesperada.");
    return corpo;
}

export function mensagem(texto, erro = false) {
    const caixa = document.querySelector("#mensagem");
    caixa.textContent = texto;
    caixa.classList.toggle("erro", erro);
    caixa.hidden = !texto;
    if (erro) {
        caixa.focus();
        caixa.scrollIntoView({ behavior: "auto", block: "nearest" });
    }
}

export function avisarDepois(texto) {
    sessionStorage.setItem("agenda-campus-aviso", texto);
}

const aviso = sessionStorage.getItem("agenda-campus-aviso");
if (aviso) {
    mensagem(aviso);
    sessionStorage.removeItem("agenda-campus-aviso");
}

// Bloqueia apenas a ação em andamento, sem apagar o formulário em caso de erro.
export async function executar(botao, acao) {
    if (botao?.disabled) return;
    if (botao) botao.disabled = true;
    try {
        await acao();
    } catch (erro) {
        mensagem(erro.message, true);
    } finally {
        if (botao) botao.disabled = false;
    }
}

// Ex.: contagem(1, "vaga", "vagas") → "1 vaga"; contagem(3, "vaga", "vagas") → "3 vagas".
export function contagem(quantidade, singular, plural) {
    return quantidade + " " + (quantidade === 1 ? singular : plural);
}

export function dataFormatada(valor, comHora = true) {
    return new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "medium", ...(comHora ? { timeStyle: "short" } : {})
    }).format(new Date(valor));
}

export function inscricoesAbertas(evento) {
    const agora = Date.now();
    return new Date(evento.dataInicio).getTime() > agora
        && new Date(evento.prazoInscricao).getTime() > agora;
}

export function dataLocal(valor) {
    const data = new Date(valor);
    const completar = numero => String(numero).padStart(2, "0");
    return data.getFullYear() + "-" + completar(data.getMonth() + 1) + "-" + completar(data.getDate())
        + "T" + completar(data.getHours()) + ":" + completar(data.getMinutes())
        + ":" + completar(data.getSeconds());
}

// IDs da API são inteiros positivos, sem zeros à esquerda.
export function idValido(valor) {
    return /^[1-9]\d*$/.test(valor ?? "");
}

export function idDaPagina() {
    const id = new URLSearchParams(location.search).get("id");
    if (!idValido(id)) throw new Error("Informe um ID válido para abrir este registro.");
    return id;
}

