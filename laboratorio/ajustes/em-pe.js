/* AJUSTES — em pé (toque). Os mesmos grupos e ajustes da versão deitada.
   Tocar num grupo entra nele; tocar num ajuste com valores abre a lista de
   valores ali mesmo. "Auto" mostra o que resolveu (P-37). */
import "./em-pe.css";
import { montarTela, esc, irPara } from "../comum/tela.js";
import { iniciarFoco, definirEntrada } from "../comum/foco.js";
import { GRUPOS } from "./grupos.js";
import { montarRefinamento } from "./refinamento.js";

const estado = Object.fromEntries(GRUPOS.flatMap(([, , itens]) => itens.filter((i) => i.valores).map((i) => [i.id, 0])));
let grupo = null;
let aberto = null;

const tela = montarTela({
  secao: "ajustes",
  classe: "ajustes-pe",
  conteudo: `<div class="pe-rolagem" data-conteudo></div>`,
});

function desenhar() {
  const c = tela.querySelector("[data-conteudo]");
  if (!grupo) {
    c.innerHTML = `
      <header class="pe-cabeca"><h1>Ajustes</h1></header>
      <nav class="pe-grupos" data-lab="grupos">
        ${GRUPOS.map(([id, nome, itens]) => `<button type="button" class="pe-grupo" data-foco data-grupo="${id}"><span><b>${esc(nome)}</b><small>${esc(itens.slice(0, 2).map((i) => i.nome).join(" · "))}</small></span><span class="seta">›</span></button>`).join("")}
      </nav>`;
    c.querySelectorAll("[data-grupo]").forEach((b) => b.addEventListener("click", () => { grupo = b.dataset.grupo; desenhar(); }));
    return;
  }
  const [, nome, itens] = GRUPOS.find(([id]) => id === grupo);
  c.innerHTML = `
    <header class="pe-sub"><button type="button" class="voltar-pe" data-foco data-voltar-grupo aria-label="Voltar">‹</button><h1>${esc(nome)}</h1></header>
    <div class="pe-itens" data-lab="itens">
      ${itens.map((i) => `
        <div class="pe-ajuste">
          <button type="button" class="pe-aj-linha" data-foco data-ajuste="${i.id}">
            <span><b>${esc(i.nome)}</b>${i.nota ? `<small>${esc(i.nota)}</small>` : ""}</span>
            <span class="pe-valor">${esc(i.valores ? i.valores[estado[i.id]] : i.acao)}</span>
          </button>
          ${i.valores && aberto === i.id ? `<div class="pe-valores" role="radiogroup">${i.valores.map((v, n) => `<button type="button" role="radio" aria-checked="${n === estado[i.id]}" data-foco data-valor="${n}" data-de="${i.id}"><span class="radio"></span>${esc(v)}</button>`).join("")}</div>` : ""}
        </div>`).join("")}
    </div>`;
  c.querySelector("[data-voltar-grupo]").addEventListener("click", () => { grupo = null; aberto = null; desenhar(); });
  c.querySelectorAll("[data-ajuste]").forEach((b) => b.addEventListener("click", () => {
    const item = itens.find((i) => i.id === b.dataset.ajuste);
    if (!item.valores) return;
    aberto = aberto === item.id ? null : item.id;
    desenhar();
  }));
  c.querySelectorAll("[data-valor]").forEach((b) => b.addEventListener("click", () => {
    estado[b.dataset.de] = +b.dataset.valor;
    if (b.dataset.de === "glifos") definirEntrada(["toque", "xbox", "ps", "toque"][estado.glifos]);
    aberto = null;
    desenhar();
  }));
}

desenhar();
iniciarFoco({ b: () => (grupo ? ((grupo = null), (aberto = null), desenhar()) : irPara("inicio")) });
montarRefinamento();
