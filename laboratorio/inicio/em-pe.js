/* INÍCIO — em pé (toque). O mesmo conteúdo da versão deitada, na forma do
   celular: o jogo de "continuar" em cima com o Jogar ao alcance, os outros
   recentes numa lista com o estado escrito e a ação de cada um no lugar
   (P-26). Tocar na linha abre os detalhes; o ⋯ abre as mesmas Opções do ☰. */
import "./inicio.css";
import "./em-pe.css";
import { PORTS, RECENTES, porId } from "../comum/dados.js";
import { montarTela, arte, esc, estadoHtml, estadoDe, irPara, montarOpcoes, abrirOpcoes, fecharOpcoes, executarOpcao, acaoPrincipal } from "../comum/tela.js";
import { iniciarFoco } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

const [primeiro, ...outros] = RECENTES.map(porId);
const pe = estadoDe(primeiro);

const linha = (p) => {
  const e = estadoDe(p);
  return `
  <div class="linha-port">
    <button type="button" class="lp-abrir" data-foco data-detalhes="${p.id}">
      <span ${arte(p, "lp-capa")}></span>
      <span class="lp-texto"><b>${esc(p.titulo)}</b>${estadoHtml(p, { longo: false })}${p.estado === "baixando" ? `<span class="barra"><i style="width:${p.progresso}%"></i></span>` : ""}</span>
    </button>
    <button type="button" class="btn ${p.estado === "falta-arquivo" ? "primario" : "contorno"} lp-acao" data-foco data-acao="${p.id}">${esc(p.estado === "baixando" ? "Jogar" : e.acao === "Escolher arquivo" ? "Escolher" : e.acao)}</button>
    <button type="button" class="lp-mais" data-foco data-mais="${p.id}" aria-label="Opções de ${esc(p.titulo)}">⋯</button>
  </div>`;
};

const tela = montarTela({
  secao: "inicio",
  classe: "inicio-pe",
  conteudo: `
    <div class="pe-rolagem">
      <header class="pe-cabeca"><h1>Início</h1><button type="button" class="ibtn" data-foco data-buscar aria-label="Buscar">⌕</button></header>
      <section class="pe-hero" data-lab="destaque">
        <div ${arte(primeiro, "pe-hero-arte")}></div>
        <div class="pe-hero-veu"></div>
        <div class="pe-hero-info">
          <div class="sobre">Continuar jogando</div>
          <div class="logo">${esc(primeiro.logo)}</div>
          ${estadoHtml(primeiro)}
          <div class="acoes">
            <button type="button" class="btn primario" data-foco data-acao="${primeiro.id}">▶ ${esc(pe.acao)}</button>
            <button type="button" class="btn contorno quadrado" data-foco data-mais="${primeiro.id}" aria-label="Opções">⋯</button>
          </div>
        </div>
      </section>
      <h2 class="pe-titulo">Recentes</h2>
      <div class="pe-lista" data-lab="lista">${outros.map(linha).join("")}</div>
      <button type="button" class="pe-todos" data-foco data-todos>Ver todos os ${PORTS.length} ports <span>›</span></button>
    </div>`,
});
montarOpcoes(tela);

tela.querySelectorAll("[data-acao]").forEach((b) => b.addEventListener("click", () => acaoPrincipal(porId(b.dataset.acao))()));
tela.querySelectorAll("[data-detalhes]").forEach((b) => b.addEventListener("click", () => irPara("detalhes", `?port=${b.dataset.detalhes}`)));
tela.querySelectorAll("[data-mais]").forEach((b) => b.addEventListener("click", () => abrirOpcoes(porId(b.dataset.mais), executarOpcao)));
tela.querySelector("[data-todos]").addEventListener("click", () => irPara("ports"));
tela.querySelector("[data-buscar]").addEventListener("click", () => irPara("ports", "?buscar=1"));

/* segurar o dedo numa linha também abre as Opções — é atalho, nunca o único
   caminho (regra do Roseta: nada só por toque longo) */
tela.querySelectorAll("[data-detalhes]").forEach((b) => {
  let t;
  b.addEventListener("pointerdown", () => (t = setTimeout(() => abrirOpcoes(porId(b.dataset.detalhes), executarOpcao), 550)));
  ["pointerup", "pointerleave", "pointercancel"].forEach((ev) => b.addEventListener(ev, () => clearTimeout(t)));
});

iniciarFoco({ inicial: tela.querySelector("[data-acao]"), b: () => fecharOpcoes() });
montarRefinamento();
