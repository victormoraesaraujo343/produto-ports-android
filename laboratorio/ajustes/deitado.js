/* AJUSTES — deitado. Poucos ajustes, agrupados por assunto. "Auto" sempre
   mostra o valor que resolveu (P-37). A pela linha troca o valor; nada de
   submenu pra uma escolha só. */
import "./ajustes.css";
import { montarTela, esc, trocarDicas, secaoVizinha, irPara } from "../comum/tela.js";
import { iniciarFoco, focar, definirEntrada } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";
import { GRUPOS } from "./grupos.js";

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
