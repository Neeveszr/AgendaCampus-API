import { contagem } from "./comum.js";

// Dados da API entram sempre como texto, nunca como HTML.
export function elemento(tag, classe = "", texto = "") {
    const item = document.createElement(tag);
    item.className = classe;
    item.textContent = texto;
    return item;
}

export function link(texto, destino, classe = "") {
    const item = elemento("a", classe, texto);
    item.href = destino;
    return item;
}

export function botao(texto, classe, aoClicar) {
    const item = elemento("button", classe, texto);
    item.type = "button";
    item.addEventListener("click", () => aoClicar(item));
    return item;
}

export function statusInscricao(status) {
    return elemento("span", "badge " + (status === "CONFIRMADA" ? "green" : ""),
        status === "CONFIRMADA" ? "Confirmada" : "Cancelada");
}

export function linhaVazia(tabela, colunas, texto = "Nenhum resultado encontrado.") {
    const linha = elemento("tr");
    const celula = elemento("td", "empty", texto);
    celula.colSpan = colunas;
    linha.append(celula);
    tabela.replaceChildren(linha);
}

export function paginacao(pagina, mudarPagina) {
    const area = document.querySelector("#paginacao");
    const resumo = elemento("span", "", contagem(pagina.totalElements, "resultado", "resultados"));
    const controles = elemento("div", "actions");
    const anterior = botao("← Anterior", "btn subtle", () => mudarPagina(pagina.number - 1));
    const proxima = botao("Próxima →", "btn subtle", () => mudarPagina(pagina.number + 1));
    anterior.disabled = pagina.first;
    proxima.disabled = pagina.last;
    controles.append(anterior, elemento("span", "", "Página " + (pagina.number + 1) + " de " + Math.max(1, pagina.totalPages)), proxima);
    area.replaceChildren(resumo, controles);
}

export function parametros(formulario) {
    const busca = new URLSearchParams();
    for (const [chave, valor] of new FormData(formulario)) {
        if (String(valor).trim()) busca.set(chave, String(valor).trim());
    }
    return busca;
}

