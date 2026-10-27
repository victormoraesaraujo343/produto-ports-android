/* PORTS — em pé (toque). Mesma lista, mesmos filtros e mesma ordem da versão
   deitada. Sem foco pra dar contexto, o nome e o estado ficam embaixo de
   cada capa. Busca usa o teclado do próprio Android. */
import "./ports.css";
import "./em-pe.css";
import { PORTS, porId } from "../comum/dados.js";
import { montarTela, arte, esc, estadoDe, seloHtml, irPara, montarOpcoes, abrirOpcoes, fecharOpcoes, executarOpcao } from "../comum/tela.js";
import { iniciarFoco } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

const instalado = (p) => !["disponivel", "nao-roda"].includes(p.estado);
const FILTROS = [
  ["todos", "Todos", () => true],
  ["instalados", "Instalados", instalado],
  ["disponiveis", "Disponíveis", (p) => !instalado(p)],
  ["atualizacoes", "Atualizações", (p) => p.estado === "atualizar" || p.estado === "baixando"],
];
const params = new URLSearchParams(location.search);
let filtro = params.get("filtro") ?? "todos";
let busca = "";

const tela = montarTela({
  secao: "ports",
  classe: "ports-pe",
  conteudo: `
    <div class="pe-rolagem">
      <header class="pe-cabeca" data-lab="filtros">
        <h1>Ports</h1>
        <label class="pe-busca"><span aria-hidden="true">⌕</span><input type="search" placeholder="Buscar jogo ou port" data-busca aria-label="Buscar"></label>
      </header>
      <div class="pe-chips" role="toolbar" aria-label="Filtros">
        ${FILTROS.map(([id, nome, f]) => `<button type="button" class="chip" data-foco data-filtro="${id}">${nome} <span class="n">${PORTS.filter(f).length}</span></button>`).join("")}
      </div>
      <div class="pe-ordem">A–Z</div>
      <section class="pe-grade" data-lab="grade" data-grade></section>
      <p class="vazio" data-vazio hidden>Nenhum port com esse nome.</p>
    </div>`,
});
montarOpcoes(tela);

function desenhar() {
  const f = FILTROS.find(([id]) => id === filtro)[2];
  const q = busca.toLowerCase();
  const lista = PORTS.filter(f).filter((p) => !q || p.titulo.toLowerCase().includes(q) || p.port.toLowerCase().includes(q)).sort((a, b) => a.titulo.localeCompare(b.titulo, "pt-BR"));
  tela.querySelector("[data-grade]").innerHTML = lista.map((p) => `
    <button type="button" class="pe-capa" data-foco data-port="${p.id}">
      <span class="vcapa"><span ${arte(p, "vcapa-arte")}></span>${p.cor ? "" : `<span class="vcapa-titulo">${esc(p.titulo)}</span>`}${seloHtml(p)}${p.estado === "baixando" ? `<span class="barra vcapa-barra"><i style="width:${p.progresso}%"></i></span>` : ""}</span>
      <span class="pe-nome">${esc(p.titulo)}</span>
      <span class="pe-estado ${estadoDe(p).tom}">${esc(estadoDe(p).curto)}</span>
    </button>`).join("");
  tela.querySelector("[data-vazio]").hidden = lista.length > 0;
  tela.querySelectorAll("[data-filtro]").forEach((b) => b.setAttribute("aria-pressed", b.dataset.filtro === filtro));
  tela.querySelectorAll("[data-port]").forEach((b) => {
    b.addEventListener("click", () => irPara("detalhes", `?port=${b.dataset.port}`));
    let t;
    b.addEventListener("pointerdown", () => (t = setTimeout(() => abrirOpcoes(porId(b.dataset.port), executarOpcao), 550)));
    ["pointerup", "pointerleave", "pointercancel"].forEach((ev) => b.addEventListener(ev, () => clearTimeout(t)));
  });
}

tela.querySelectorAll("[data-filtro]").forEach((b) => b.addEventListener("click", () => { filtro = b.dataset.filtro; desenhar(); }));
const campo = tela.querySelector("[data-busca]");
campo.addEventListener("input", () => { busca = campo.value; desenhar(); });
if (params.get("buscar")) setTimeout(() => campo.focus(), 50);

desenhar();
iniciarFoco({ b: () => fecharOpcoes() });
montarRefinamento();
