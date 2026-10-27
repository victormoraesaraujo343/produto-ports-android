/* INÍCIO — deitado. O jogo em foco domina a tela (P-06); a fileira é atalho
   pros recentes, a biblioteca inteira fica pra baixo (Síntese §1, decisão Q-c).
   O Jogar está no próprio jogo em foco: A joga, Y abre detalhes (Síntese §3). */
import "./inicio.css";
import { PORTS, RECENTES, porId } from "../comum/dados.js";
import { montarTela, arte, esc, g, estadoHtml, estadoDe, seloHtml, trocarDicas, secaoVizinha, irPara, montarOpcoes, abrirOpcoes, fecharOpcoes, executarOpcao, acaoPrincipal } from "../comum/tela.js";
import { iniciarFoco } from "../comum/foco.js";
import { montarRefinamento, aoMudar, variante } from "./refinamento.js";

const recentes = RECENTES.map(porId);

const tile = (p, i) => `
  <button type="button" class="capa" id="r-${p.id}" data-foco data-port="${p.id}" aria-label="${esc(p.titulo)} — ${esc(estadoDe(p).curto)}">
    <span ${arte(p, "capa-arte")}></span>
    ${seloHtml(p)}
    ${p.estado === "baixando" ? `<span class="barra capa-barra"><i style="width:${p.progresso}%"></i></span>` : ""}
    <span class="capa-nome">${esc(p.titulo)}</span>
  </button>`;

const tela = montarTela({
  secao: "inicio",
  classe: "inicio",
  conteudo: `
    <section class="destaque" data-lab="destaque" aria-live="polite">
      <div class="fundo" data-fundo="a"></div><div class="fundo" data-fundo="b"></div>
      <div class="veu"></div>
      <div class="info" data-info></div>
    </section>
    <section class="recentes" data-lab="recentes" aria-label="Recentes">
      <div class="trilho" data-rolagem="40">
        ${recentes.map(tile).join("")}
        <button type="button" class="capa todos" id="r-todos" data-foco data-todos>
          <span>Todos os ports</span><span class="seta">↓</span><small>${PORTS.length} no catálogo</small>
        </button>
      </div>
    </section>`,
});
montarOpcoes(tela);

/* ------------------------------------------------------------ destaque = o jogo em foco */
let fundoAtivo = "a";
function mostrar(p) {
  const info = tela.querySelector("[data-info]");
  if (!p) {
    trocarFundo(null);
    info.innerHTML = `
      <div class="sobre">Biblioteca</div>
      <div class="logo">TODOS OS PORTS</div>
      <div class="meta">${PORTS.length} ports no catálogo · ${PORTS.filter((x) => x.estado !== "disponivel" && x.estado !== "nao-roda").length} instalados</div>
      <div class="acoes"><button type="button" class="btn primario" data-ir-ports>${g("a")}Abrir a lista</button></div>`;
    info.querySelector("[data-ir-ports]").onclick = () => irPara("ports");
    return trocarDicas([["a", "Abrir"], ["b", "Voltar"]]);
  }
  const e = estadoDe(p);
  trocarFundo(p);
  const sobre = p.id === recentes[0].id ? "Continuar jogando" : p.jogadoHa && p.jogadoHa !== "nunca jogado" ? `Jogado ${p.jogadoHa}` : "Recentes";
  info.innerHTML = `
    <div class="sobre">${esc(sobre)}</div>
    <div class="logo">${esc(p.logo)}</div>
    <div class="meta">${esc(p.port)} · ${esc(p.tipo)}${p.versao !== "—" ? ` · v${esc(p.versao.split(" ")[0])}` : ""}</div>
    <div class="linha-estado">${estadoHtml(p)}</div>
    ${p.estado === "falta-arquivo" ? `<p class="nota">Serve: ${esc(p.arquivo)}. A gente confere o arquivo pra você.</p>` : ""}
    ${p.estado === "baixando" ? `<div class="barra larga"><i style="width:${p.progresso}%"></i></div>` : ""}
    <div class="acoes">
      <button type="button" class="btn primario" data-principal>${g("a")}${esc(e.acao)}</button>
      <button type="button" class="btn contorno" data-detalhes>${g("y")}Detalhes</button>
      <button type="button" class="btn contorno so-toque" data-mais aria-label="Opções">⋯</button>
    </div>`;
  info.querySelector("[data-principal]").onclick = () => acaoPrincipal(p)();
  info.querySelector("[data-detalhes]").onclick = () => irPara("detalhes", `?port=${p.id}`);
  info.querySelector("[data-mais]").onclick = () => abrirOpcoes(p, executarOpcao);
  trocarDicas([["a", e.acao], ["y", "Detalhes"], ["menu", "Opções"]]);
}

function trocarFundo(p) {
  const prox = fundoAtivo === "a" ? "b" : "a";
  const el = tela.querySelector(`[data-fundo="${prox}"]`);
  el.className = "fundo";
  if (p?.cor) { el.classList.add("arte"); el.style.cssText = `--c1:${p.cor[0]};--c2:${p.cor[1]};--c3:${p.cor[2]}`; }
  else { el.classList.add("sem-arte"); el.style.cssText = ""; }
  el.classList.add("visivel");
  tela.querySelector(`[data-fundo="${fundoAtivo}"]`).classList.remove("visivel");
  fundoAtivo = prox;
}

tela.addEventListener("focado", (ev) => {
  const el = ev.target;
  if (el.dataset.port && !el.closest("[data-camada]")) mostrar(porId(el.dataset.port));
  else if (el.dataset.todos !== undefined) mostrar(null);
});
tela.querySelectorAll("[data-port]").forEach((b) => b.addEventListener("click", () => acaoPrincipal(porId(b.dataset.port))()));
tela.querySelector("[data-todos]").addEventListener("click", () => irPara("ports"));

const portFocado = (el) => el?.dataset.port && porId(el.dataset.port);
iniciarFoco({
  inicial: tela.querySelector(".capa"),
  y: (el) => portFocado(el) && irPara("detalhes", `?port=${el.dataset.port}`),
  menu: (el) => portFocado(el) && abrirOpcoes(portFocado(el), executarOpcao),
  b: () => fecharOpcoes(),
  l1: () => secaoVizinha("inicio", -1),
  r1: () => secaoVizinha("inicio", 1),
  borda: (dir) => dir === "baixo" && irPara("ports"),
});

aoMudar("destaque", () => {});
montarRefinamento();
