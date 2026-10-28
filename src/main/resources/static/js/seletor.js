import { requisicao, mensagem, contagem } from "./comum.js";

// Mesmo texto para as opções carregadas pela busca e para a escolhida pela URL.
const rotulo = item => item.nome + (item.email ? " — " + item.email : " (#" + item.id + ")");

// Pesquisa paginada para os selects de eventos e participantes.
export function criarSeletor(id, recurso, obrigatorio = false) {
    const area = document.getElementById(id);
    const select = area.querySelector("select");
    const busca = area.querySelector("input");
    const buscar = area.querySelector(".buscar");
    const mais = area.querySelector(".mais");
    const estado = area.querySelector(".seletor-estado");
    let pagina = -1;
    let termo = "";
    let versao = 0;

    async function carregar(reiniciar = true) {
        if (!reiniciar && pagina < 0) return;
        const atual = ++versao;
        const proxima = reiniciar ? 0 : pagina + 1;
        if (reiniciar) {
            pagina = -1;
            mais.hidden = true;
            termo = busca.value.trim();
            select.replaceChildren(new Option(obrigatorio ? "Selecione" : "Todos", ""));
            select.dispatchEvent(new Event("change"));
        }
        buscar.disabled = mais.disabled = select.disabled = true;
        estado.textContent = "Carregando opções…";
        try {
            const consulta = new URLSearchParams({ nome: termo, page: proxima, size: 10, sort: "nome,asc" });
            const resultado = await requisicao("/" + recurso + "?" + consulta);
            if (atual !== versao) return;
            resultado.content.forEach(item => select.add(new Option(rotulo(item), item.id)));
            pagina = proxima;
            mais.hidden = resultado.last;
            estado.textContent = resultado.empty ? "Nenhum resultado. Altere a busca." : contagem(resultado.totalElements, "resultado", "resultados") + ".";
        } catch (erro) {
            if (atual === versao) {
                estado.textContent = "Falha ao carregar. Use Buscar para tentar novamente.";
                mensagem(erro.message, true);
            }
        } finally {
            if (atual === versao) {
                buscar.disabled = select.disabled = false;
                mais.disabled = pagina < 0;
            }
        }
    }

    buscar.addEventListener("click", () => carregar());
    mais.addEventListener("click", () => carregar(false));
    busca.addEventListener("keydown", evento => {
        if (evento.key === "Enter") {
            evento.preventDefault();
            carregar();
        }
    });
    const pronto = carregar();
    return {
        select, pronto,
        limpar() { busca.value = ""; return carregar(); },
        async selecionar(id) {
            // Permite abrir um filtro vindo dos detalhes mesmo fora da primeira página.
            const item = await requisicao("/" + recurso + "/" + id);
            if (![...select.options].some(opcao => opcao.value === String(id))) select.add(new Option(rotulo(item), id));
            select.value = String(id);
            select.dispatchEvent(new Event("change"));
        }
    };
}

