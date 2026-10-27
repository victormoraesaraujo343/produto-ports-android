/* AJUSTES — deitado. Poucos ajustes, agrupados por assunto. "Auto" sempre
   mostra o valor que resolveu (P-37). A pela linha troca o valor; nada de
   submenu pra uma escolha só. */
import "./ajustes.css";
import { montarTela, esc, trocarDicas, secaoVizinha, irPara } from "../comum/tela.js";
import { iniciarFoco, focar, definirEntrada } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

const GRUPOS = [
  ["atualizacoes", "Atualizações", [
    { id: "auto", nome: "Atualizar ports sozinho", valores: ["Só no Wi‑Fi", "Sempre", "Perguntar antes"], nota: "Atualiza em segundo plano os ports que este app instalou." },
    { id: "teste", nome: "Versões de teste", valores: ["Não", "Sim"], nota: "Algumas versões novas saem primeiro em teste e podem ter defeitos." },
    { id: "agora", nome: "Procurar agora", acao: "Última vez: hoje, 08:12" },
  ]],
  ["controles", "Controles e botões", [
    { id: "glifos", nome: "Botões mostrados na tela", valores: ["Auto (Xbox)", "Xbox", "PlayStation", "Esconder"], nota: "Auto segue o controle que você está usando." },
    { id: "confirmar", nome: "Botão de confirmar", valores: ["Auto (A embaixo)", "Embaixo", "À direita"], nota: "Pra controles estilo Nintendo, que trocam A e B de lugar." },
    { id: "vibrar", nome: "Vibração ao navegar", valores: ["Desligada", "Ligada"] },
  ]],
  ["arquivos", "Arquivos e espaço", [
    { id: "pasta", nome: "Pasta dos seus jogos", valores: ["Downloads/Jogos", "Escolher outra…"], nota: "O app procura aqui o arquivo que cada port precisa." },
    { id: "espaco", nome: "Espaço usado pelos ports", acao: "412 MB em 5 ports" },
    { id: "limpar", nome: "Apagar downloads que sobraram", acao: "38 MB" },
  ]],
  ["catalogo", "Catálogo", [
    { id: "fonte", nome: "De onde vem a lista", acao: "ports-catalogue (GitHub) ↗" },
    { id: "naotestados", nome: "Mostrar ports ainda não testados", valores: ["Sim", "Não"], nota: "Ports que entraram na lista sozinhos, sem ninguém testar." },
    { id: "naoroda", nome: "Mostrar ports que não rodam aqui", valores: ["Sim, marcados", "Não"] },
  ]],
  ["sobre", "Sobre", [
    { id: "versao", nome: "Versão do app", acao: "0.1 (lab)" },
    { id: "jogos", nome: "Este app não contém jogos", acao: "Cada port precisa do seu jogo original." },
    { id: "licencas", nome: "Licenças", acao: "Ver ↗" },
  ]],
];
const estado = Object.fromEntries(GRUPOS.flatMap(([, , itens]) => itens.filter((i) => i.valores).map((i) => [i.id, 0])));

const tela = montarTela({
  secao: "ajustes",
  classe: "ajustes",
  conteudo: `
    <nav class="grupos" data-lab="grupos" aria-label="Grupos de ajustes">
      ${GRUPOS.map(([id, nome]) => `<button type="button" class="grupo" id="g-${id}" data-foco data-grupo="${id}">${esc(nome)}</button>`).join("")}
    </nav>
    <section class="itens" data-lab="itens" data-rolagem="16" data-itens></section>`,
});

let grupoAtual = GRUPOS[0][0];
function desenharItens() {
  const [, nome, itens] = GRUPOS.find(([id]) => id === grupoAtual);
  tela.querySelectorAll("[data-grupo]").forEach((b) => b.setAttribute("aria-current", b.dataset.grupo === grupoAtual));
  tela.querySelector("[data-itens]").innerHTML = `
    <h2>${esc(nome)}</h2>
    ${itens.map((i) => `
      <button type="button" class="ajuste" id="a-${i.id}" data-foco data-ajuste="${i.id}">
        <span class="aj-texto"><b>${esc(i.nome)}</b>${i.nota ? `<small>${esc(i.nota)}</small>` : ""}</span>
        <span class="aj-valor">${i.valores ? `<span class="seta">‹</span>${esc(i.valores[estado[i.id]])}<span class="seta">›</span>` : esc(i.acao)}</span>
      </button>`).join("")}`;
  tela.querySelectorAll("[data-ajuste]").forEach((b) => b.addEventListener("click", () => trocar(b.dataset.ajuste, 1)));
}
function trocar(id, passo) {
  const item = GRUPOS.flatMap(([, , i]) => i).find((i) => i.id === id);
  if (!item?.valores) return;
  estado[id] = (estado[id] + passo + item.valores.length) % item.valores.length;
  if (id === "glifos") definirEntrada(["xbox", "xbox", "ps", "toque"][estado[id]]);
  desenharItens();
  focar(tela.querySelector(`#a-${id}`), { rolar: false });
}

tela.querySelectorAll("[data-grupo]").forEach((b) => {
  b.addEventListener("click", () => { grupoAtual = b.dataset.grupo; desenharItens(); focar(tela.querySelector("[data-ajuste]")); });
  b.addEventListener("focado", () => { grupoAtual = b.dataset.grupo; desenharItens(); });
});
tela.addEventListener("focado", (ev) => {
  const el = ev.target;
  if (el.dataset.grupo) return trocarDicas([["a", "Abrir"], ["b", "Início"]]);
  const item = GRUPOS.flatMap(([, , i]) => i).find((i) => i.id === el.dataset.ajuste);
  trocarDicas(item?.valores ? [["a", "Trocar"], ["b", "Voltar"]] : [["a", "Abrir"], ["b", "Voltar"]]);
});

desenharItens();
iniciarFoco({
  inicial: tela.querySelector("[data-grupo]"),
  mover: (dir, el) => {
    /* na linha de um ajuste, ← → trocam o valor; ← no começo volta aos grupos */
    if (el?.dataset.ajuste && (dir === "esquerda" || dir === "direita")) {
      const item = GRUPOS.flatMap(([, , i]) => i).find((i) => i.id === el.dataset.ajuste);
      if (dir === "esquerda" && (!item.valores || estado[item.id] === 0)) { focar(tela.querySelector(`[data-grupo="${grupoAtual}"]`)); return false; }
      if (item.valores) { trocar(item.id, dir === "direita" ? 1 : -1); return false; }
      return false;
    }
  },
  b: (el) => (el?.dataset.ajuste ? focar(tela.querySelector(`[data-grupo="${grupoAtual}"]`)) : irPara("inicio")),
  l1: () => secaoVizinha("ajustes", -1),
  r1: () => secaoVizinha("ajustes", 1),
});
montarRefinamento();
