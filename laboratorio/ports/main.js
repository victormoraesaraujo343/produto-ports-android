/* PORTS — deitado. A biblioteca inteira, em ordem A–Z (Síntese §2: ordem
   estável ajuda quem volta, sem custo pra quem chega). Filtros pelo que o app
   já sabe; nada de pasta manual. O que está em foco aparece na faixa de
   baixo: contexto na seleção, sem abrir outra tela (Síntese §3, P-30). */
import "./ports.css";
import { PORTS, porId } from "../comum/dados.js";
import { montarTela, arte, esc, g, estadoHtml, estadoDe, seloHtml, trocarDicas, secaoVizinha, irPara, montarOpcoes, abrirOpcoes, fecharOpcoes, executarOpcao, acaoPrincipal } from "../comum/tela.js";
import { iniciarFoco, focar, abrirCamada, fecharCamada, camadaAberta, trocarTratadores } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

const instalado = (p) => !["disponivel", "nao-roda"].includes(p.estado);
const FILTROS = [
  ["todos", "Todos", () => true],
  ["instalados", "Instalados", instalado],
  ["disponiveis", "Disponíveis", (p) => !instalado(p)],
  ["atualizacoes", "Atualizações", (p) => p.estado === "atualizar" || p.estado === "baixando"],
];
const ORDENS = [
  ["az", "A–Z", (a, b) => a.titulo.localeCompare(b.titulo, "pt-BR")],
  ["plataforma", "Plataforma", (a, b) => a.plataforma.localeCompare(b.plataforma) || a.titulo.localeCompare(b.titulo, "pt-BR")],
];
const params = new URLSearchParams(location.search);
let filtro = params.get("filtro") ?? "todos";
let ordem = 0;
let busca = "";

const capa = (p) => `
  <button type="button" class="vcapa" id="p-${p.id}" data-foco data-port="${p.id}" aria-label="${esc(p.titulo)} — ${esc(estadoDe(p).curto)}">
    <span ${arte(p, "vcapa-arte")}></span>
    ${p.cor ? "" : `<span class="vcapa-titulo">${esc(p.titulo)}</span>`}
    ${seloHtml(p)}
    ${p.estado === "baixando" ? `<span class="barra vcapa-barra"><i style="width:${p.progresso}%"></i></span>` : ""}
  </button>`;

const tela = montarTela({
  secao: "ports",
  classe: "ports",
  conteudo: `
    <div class="filtros" data-lab="filtros" role="toolbar" aria-label="Filtros">
      ${FILTROS.map(([id, nome, f]) => `<button type="button" class="chip" id="f-${id}" data-foco data-filtro="${id}">${nome} <span class="n">${PORTS.filter(f).length}</span></button>`).join("")}
      <span class="espaco"></span>
      <button type="button" class="chip leve" id="f-ordem" data-foco data-ordem>Ordem: <b data-ordem-nome>A–Z</b></button>
      <button type="button" class="chip leve" id="f-busca" data-foco data-abrir-busca>⌕ Buscar</button>
    </div>
    <section class="grade-caixa" data-lab="grade" data-rolagem="16">
      <div class="busca-ativa" data-busca-ativa hidden></div>
      <div class="grade" data-grade></div>
      <p class="vazio" data-vazio hidden>Nenhum port com esse nome. <button type="button" class="btn contorno" data-foco data-limpar>Limpar busca</button></p>
    </section>
    <footer class="contexto" data-lab="contexto" data-contexto></footer>
    ${tecladoHtml()}`,
});
montarOpcoes(tela);

/* ------------------------------------------------------------ grade */
function desenhar() {
  const f = FILTROS.find(([id]) => id === filtro)[2];
  const lista = PORTS.filter(f).filter((p) => !busca || p.titulo.toLowerCase().includes(busca.toLowerCase()) || p.port.toLowerCase().includes(busca.toLowerCase())).sort(ORDENS[ordem][2]);
  tela.querySelector("[data-grade]").innerHTML = lista.map(capa).join("");
  tela.querySelector("[data-vazio]").hidden = lista.length > 0;
  const ba = tela.querySelector("[data-busca-ativa]");
  ba.hidden = !busca;
  ba.innerHTML = busca ? `Buscando “${esc(busca)}” · ${lista.length} ${lista.length === 1 ? "port" : "ports"}` : "";
  tela.querySelectorAll("[data-filtro]").forEach((b) => b.setAttribute("aria-pressed", b.dataset.filtro === filtro));
  tela.querySelector("[data-ordem-nome]").textContent = ORDENS[ordem][1];
  tela.querySelectorAll("[data-grade] [data-port]").forEach((b) => b.addEventListener("click", () => acaoPrincipal(porId(b.dataset.port))()));
  return lista;
}

function contexto(p) {
  const el = tela.querySelector("[data-contexto]");
  if (!p) { el.innerHTML = ""; return trocarDicas([["a", "Escolher"], ["b", "Voltar"]]); }
  const e = estadoDe(p);
  el.innerHTML = `
    <div class="ctx-titulo"><b>${esc(p.titulo)}</b><span>${esc(p.port)} · ${esc(p.plataforma)}</span></div>
    <div class="ctx-estado">${estadoHtml(p)}</div>`;
  trocarDicas([["a", e.acao], ["y", "Detalhes"], ["menu", "Opções"]]);
}

tela.addEventListener("focado", (ev) => {
  const el = ev.target;
  if (el.closest("[data-camada]")) return;
  contexto(el.dataset.port ? porId(el.dataset.port) : null);
});

tela.querySelectorAll("[data-filtro]").forEach((b) => b.addEventListener("click", () => {
  filtro = b.dataset.filtro;
  desenhar();
  history.replaceState(null, "", `?filtro=${filtro}`);
}));
tela.querySelector("[data-ordem]").addEventListener("click", () => { ordem = (ordem + 1) % ORDENS.length; desenhar(); });
tela.querySelector("[data-limpar]").addEventListener("click", () => { busca = ""; desenhar(); focar(tela.querySelector("[data-grade] [data-port]")); });

/* ------------------------------------------------------------ busca com teclado na tela
   Digitar é caro no portátil: busca é o caminho de reserva, não o principal
   (Síntese §2). O teclado abre na tela, navegável pelo controle. */
function tecladoHtml() {
  const linhas = ["1234567890", "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"];
  return `
    <div class="teclado" data-camada data-teclado data-lab="busca" aria-label="Buscar">
      <div class="campo"><span class="lupa">⌕</span><span data-texto></span><span class="cursor"></span></div>
      <div class="teclas">
        ${linhas.map((l) => `<div class="linha">${[...l].map((c) => `<button type="button" class="tecla" data-foco data-tecla="${c}">${c}</button>`).join("")}</div>`).join("")}
        <div class="linha">
          <button type="button" class="tecla larga" data-foco data-tecla=" ">espaço</button>
          <button type="button" class="tecla larga" data-foco data-apagar>⌫ apagar</button>
          <button type="button" class="tecla larga ok" data-foco data-buscar>Buscar</button>
        </div>
      </div>
      <div class="teclado-dicas">${g("x")} apagar · ${g("b")} fechar</div>
    </div>`;
}
const teclado = tela.querySelector("[data-teclado]");
let texto = "";
const atualizarCampo = () => (teclado.querySelector("[data-texto]").textContent = texto || "nome do jogo ou do port");
teclado.querySelectorAll("[data-tecla]").forEach((b) => b.addEventListener("click", () => { texto += b.dataset.tecla.toLowerCase(); atualizarCampo(); }));
teclado.querySelector("[data-apagar]").addEventListener("click", () => { texto = texto.slice(0, -1); atualizarCampo(); });
teclado.querySelector("[data-buscar]").addEventListener("click", () => { busca = texto.trim(); fecharCamada(teclado); desenhar(); focar(tela.querySelector("[data-grade] [data-port]") ?? tela.querySelector("[data-limpar]")); });
tela.querySelector("[data-abrir-busca]").addEventListener("click", () => { texto = busca; atualizarCampo(); abrirCamada(teclado, teclado.querySelector('[data-tecla="Q"]')); });

/* ------------------------------------------------------------ foco */
desenhar();
const portFocado = (el) => el?.dataset.port && porId(el.dataset.port);
iniciarFoco({
  inicial: tela.querySelector("[data-grade] [data-port]"),
  y: (el) => portFocado(el) && irPara("detalhes", `?port=${el.dataset.port}`),
  menu: (el) => portFocado(el) && abrirOpcoes(portFocado(el), executarOpcao),
  b: () => {
    const c = camadaAberta();
    if (c === teclado) return fecharCamada(teclado);
    if (fecharOpcoes()) return;
    if (busca) { busca = ""; desenhar(); return focar(tela.querySelector("[data-grade] [data-port]")); }
    irPara("inicio");
  },
  l1: () => secaoVizinha("ports", -1),
  r1: () => secaoVizinha("ports", 1),
});
trocarTratadores({ x: () => camadaAberta() === teclado && teclado.querySelector("[data-apagar]").click() });

montarRefinamento();
