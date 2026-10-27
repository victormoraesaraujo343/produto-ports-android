/* TELA — a moldura comum de toda página do lab: o aparelho, o topo com as
   seções (L1/R1), as dicas de botões e as peças repetidas (arte, selo de
   estado, painel de Opções). */
import "./tema.css";
import { ESTADOS } from "./dados.js";
import { focar, abrirCamada, fecharCamada, camadaAberta } from "./foco.js";

export const SECOES = [
  ["inicio", "Início"],
  ["ports", "Ports"],
  ["ajustes", "Ajustes"],
];
export const urlDa = (pagina, busca = "") => `/laboratorio/${pagina}/index.html${busca}`;
export const emPe = () => matchMedia("(max-width: 767px)").matches;

/* ------------------------------------------------------------ peças */
export const esc = (s = "") => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
export const g = (b, ombro = false) => `<i class="g${ombro ? " ombro" : ""}" data-b="${b}" aria-hidden="true"></i>`;
export const iniciais = (t) => t.replace(/[^A-Za-z0-9 ]/g, "").split(" ").filter(Boolean).slice(0, 2).map((p) => p[0]).join("").toUpperCase();
export const arte = (p, extra = "") =>
  p.cor
    ? `class="arte ${extra}" style="--c1:${p.cor[0]};--c2:${p.cor[1]};--c3:${p.cor[2]}" data-logo="${esc(p.logo)}"`
    : `class="arte sem ${extra}" data-iniciais="${iniciais(p.titulo)}"`;
export const estadoDe = (p) => ESTADOS[p.estado];
export const estadoHtml = (p, { longo = true } = {}) => {
  const e = estadoDe(p);
  const texto = p.estado === "baixando" ? `${e.longo} · ${p.progresso}%` : p.estado === "atualizar" ? `${e.longo}: ${p.novaVersao}` : longo ? e.longo : e.curto;
  return `<span class="estado ${e.tom}"><span class="s" aria-hidden="true">${e.simbolo}</span>${esc(texto)}</span>`;
};
export const seloHtml = (p, { palavra = false } = {}) => {
  const e = estadoDe(p);
  return `<span class="selo ${e.tom}" title="${esc(e.curto)}"><span aria-hidden="true">${e.simbolo}</span>${palavra ? esc(e.curto) : `<span class="sr">${esc(e.curto)}</span>`}</span>`;
};

/* ------------------------------------------------------------ moldura */
export function montarTela({ secao, conteudo, dicas = [], classe = "", caminho = null }) {
  document.body.innerHTML = `
    <div class="palco">
      <div class="tela ${classe}" data-secao="${secao ?? ""}">
        ${secao ? topoHtml(secao) : ""}
        ${caminho ? caminhoHtml(caminho) : ""}
        ${conteudo}
        <div class="dicas" data-dicas>${dicasHtml(dicas)}</div>
      </div>
      <p class="legenda-lab" aria-label="Atalhos do lab">
        <span><kbd>←↑↓→</kbd> mover</span><span><kbd>Enter</kbd> A</span><span><kbd>Esc</kbd> B</span>
        <span><kbd>Y</kbd> Y</span><span><kbd>Q</kbd><kbd>E</kbd> L1/R1</span><span><kbd>O</kbd> Opções</span>
        <span>Controle de verdade também funciona.</span><span><kbd>R</kbd> painel · <kbd>V</kbd> em pé</span>
      </p>
    </div>`;
  escalar();
  addEventListener("resize", escalar);
  document.querySelectorAll("[data-ir]").forEach((el) => el.addEventListener("click", () => irPara(el.dataset.ir)));
  return document.querySelector(".tela");
}

function topoHtml(secao) {
  return `<header class="topo" data-lab="topo">
    ${g("l1", true)}
    <nav class="secoes" aria-label="Seções">
      ${SECOES.map(([id, nome]) => `<button type="button" class="secao${id === secao ? " atual" : ""}" data-ir="${id}" ${id === secao ? 'aria-current="page"' : ""}>${nome}</button>`).join("")}
    </nav>
    ${g("r1", true)}
    <span class="relogio"><span>9:41</span><span aria-label="bateria 87%">▮▮▮ 87%</span></span>
  </header>`;
}

/* telas de dentro (detalhes, arquivo, instalar…): o caminho mostra onde se
   está e como voltar (P-35) */
function caminhoHtml(partes) {
  return `<header class="caminho" data-lab="caminho">
    <button type="button" class="voltar" data-voltar aria-label="Voltar">${g("b")}<span class="so-toque-inline">‹</span></button>
    ${partes.map((t, i) => `<span class="${i === partes.length - 1 ? "aqui" : ""}">${esc(t)}</span>`).join('<span class="sep">›</span>')}
    <span class="relogio"><span>9:41</span><span aria-label="bateria 87%">▮▮▮ 87%</span></span>
  </header>`;
}
export function voltar(padrao = "inicio") {
  if (history.length > 1 && document.referrer.startsWith(location.origin)) history.back();
  else irPara(padrao);
}

export const dicasHtml = (dicas) => dicas.map(([b, t]) => `<span>${g(b)}${esc(t)}</span>`).join("");
export function trocarDicas(dicas) {
  const el = document.querySelector("[data-dicas]");
  if (el) el.innerHTML = dicasHtml(dicas);
}

/* L1/R1: seção anterior/próxima */
export function secaoVizinha(secao, passo) {
  const i = SECOES.findIndex(([id]) => id === secao);
  const [id] = SECOES[(i + passo + SECOES.length) % SECOES.length];
  irPara(id);
}
export function irPara(pagina, busca = "") {
  location.href = urlDa(pagina, busca);
}

function escalar() {
  const tela = document.querySelector(".tela");
  if (!tela) return;
  if (emPe()) return tela.style.removeProperty("--escala");
  const s = Math.min(1.4, (innerWidth - 48) / 931, (innerHeight - 110) / 428);
  tela.style.setProperty("--escala", Math.max(0.5, s).toFixed(3));
}

/* ------------------------------------------------------------ painel de Opções (☰)
   Uma rota só pras ações secundárias, de qualquer tela (Síntese §3, P-40).
   O que sai do launcher leva ↗ (P-36). */
export function opcoesDoPort(p) {
  const base = [];
  const e = p.estado;
  if (e === "falta-arquivo" || e === "arquivo-errado") base.push(["arquivo", "Escolher arquivo do jogo"]);
  if (e === "pronto" || e === "atualizar" || e === "baixando") base.push(["jogar", "Jogar"]);
  if (e === "atualizar") base.push(["atualizar", `Atualizar pra ${p.novaVersao}`]);
  if (e === "disponivel") base.push(["instalar", `Instalar · ${p.tamanho}`]);
  base.push(["detalhes", "Detalhes do port"]);
  if (e !== "disponivel" && e !== "nao-roda") base.push(["ajustes-port", "Ajustes do port", "abre o port ↗"]);
  if (e === "pronto" || e === "atualizar") base.push(["trocar-arquivo", "Trocar arquivo do jogo"]);
  base.push(["projeto", "Página do projeto", "GitHub ↗"]);
  if (e !== "disponivel" && e !== "nao-roda") base.push(["desinstalar", "Desinstalar", "", "perigo"]);
  return base;
}

export function montarOpcoes(tela) {
  tela.insertAdjacentHTML("beforeend", `
    <aside class="lateral" data-camada data-opcoes data-lab="opcoes" aria-label="Opções">
      <div class="de">${g("menu")} Opções</div>
      <h3 data-opcoes-titulo></h3>
      <div data-opcoes-lista></div>
      <div class="rodape">${g("b")} Fechar <button type="button" class="fechar-toque btn contorno" data-fechar-opcoes style="margin-left:auto">Fechar</button></div>
    </aside>
    <div class="camada-fundo" data-fechar-opcoes></div>`);
  const painel = tela.querySelector("[data-opcoes]");
  tela.querySelectorAll("[data-fechar-opcoes]").forEach((el) => el.addEventListener("click", () => fecharOpcoes()));
  return painel;
}

export function abrirOpcoes(p, aoEscolher) {
  const painel = document.querySelector("[data-opcoes]");
  painel.querySelector("[data-opcoes-titulo]").textContent = p.titulo;
  painel.querySelector("[data-opcoes-lista]").innerHTML = opcoesDoPort(p)
    .map(([id, nome, nota = "", tom = ""]) => `<button type="button" class="opcao ${tom}" data-foco data-opcao="${id}"><span>${esc(nome)}</span>${nota ? `<small>${esc(nota)}</small>` : ""}</button>`)
    .join("");
  painel.querySelectorAll("[data-opcao]").forEach((b) => b.addEventListener("click", () => { fecharOpcoes(); aoEscolher?.(b.dataset.opcao, p); }));
  document.querySelector(".camada-fundo")?.classList.add("aberta");
  abrirCamada(painel);
}
export function fecharOpcoes() {
  const painel = document.querySelector("[data-opcoes]");
  if (!painel?.classList.contains("aberta")) return false;
  document.querySelector(".camada-fundo")?.classList.remove("aberta");
  fecharCamada(painel);
  return true;
}
export { camadaAberta, focar };

/* o que cada opção faz no lab (navega pra tela correspondente) */
export function executarOpcao(id, p) {
  const q = `?port=${p.id}`;
  ({
    jogar: () => irPara("abrindo", q),
    arquivo: () => irPara("arquivo", q),
    "trocar-arquivo": () => irPara("arquivo", q),
    detalhes: () => irPara("detalhes", q),
    instalar: () => irPara("instalar", q),
    atualizar: () => irPara("instalar", q + "&modo=atualizar"),
    desinstalar: () => irPara("detalhes", q + "&confirmar=desinstalar"),
  })[id]?.();
}

/* ação principal de um port, na posição de sempre (P-26) */
export function acaoPrincipal(p) {
  const q = `?port=${p.id}`;
  return ({
    "pronto": () => irPara("abrindo", q),
    "atualizar": () => irPara("abrindo", q),
    "baixando": () => irPara("abrindo", q),
    "falta-arquivo": () => irPara("arquivo", q),
    "arquivo-errado": () => irPara("arquivo", q),
    "disponivel": () => irPara("instalar", q),
    "nao-roda": () => irPara("detalhes", q),
  })[p.estado];
}
