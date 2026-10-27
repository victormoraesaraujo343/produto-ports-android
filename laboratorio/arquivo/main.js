/* ARQUIVO DO JOGO — deitado. O fluxo que nenhuma referência do Roseta tinha:
   o port precisa do jogo do próprio jogador. Palavras simples na tela; o
   técnico (hash, região) fica a um botão (P-32: informação e profundidade são
   eixos separados). O seletor de arquivos é o do Android (SAF), simulado. */
import "./arquivo.css";
import { porId } from "../comum/dados.js";
import { montarTela, esc, g, trocarDicas, irPara, voltar } from "../comum/tela.js";
import { iniciarFoco, focar, abrirCamada, fecharCamada, camadaAberta, trocarTratadores } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

const params = new URLSearchParams(location.search);
const p = porId(params.get("port") ?? "oot");
let passo = params.get("passo") ?? "inicio";
let escolhido = null;

const ARQUIVOS = [
  { nome: "zelda-oot-usa.z64", tamanho: "32 MB", serve: true, versao: "Ocarina of Time · EUA 1.0" },
  { nome: "zelda-oot-europa.z64", tamanho: "32 MB", serve: false, versao: "Ocarina of Time · Europa", porque: "É a versão europeia. O port precisa da americana 1.0 ou 1.2, ou da de GameCube." },
  { nome: "mario64.z64", tamanho: "8 MB", serve: false, versao: "Super Mario 64 · EUA", porque: "Esse é o Super Mario 64, não o Ocarina of Time." },
  { nome: "IMG_2041.jpg", tamanho: "3 MB", serve: false, versao: "imagem", porque: "Isso é uma foto, não um jogo." },
];

const tela = montarTela({
  classe: "arquivo fluxo-tela",
  caminho: [p.titulo, "Arquivo do jogo"],
  conteudo: `
    <div class="fluxo-arte arte" style="--c1:${p.cor?.[0] ?? "#333"};--c2:${p.cor?.[1] ?? "#222"};--c3:${p.cor?.[2] ?? "#111"}"></div>
    <div class="fluxo-veu"></div>
    <section class="fluxo" data-lab="fluxo" data-fluxo></section>
    <aside class="passos" data-lab="passos" data-passos></aside>
    <div class="seletor" data-camada data-seletor data-lab="seletor" aria-label="Seletor de arquivos do Android">
      <div class="sel-barra"><span class="sel-menu">≡</span><b>Downloads</b><span class="sel-busca">⌕</span></div>
      <div class="sel-aviso">Abrir com: <b>Ports</b> · seletor do Android (simulado)</div>
      <div class="sel-lista">
        ${ARQUIVOS.map((a, i) => `<button type="button" class="sel-item" data-foco data-arq="${i}"><span class="sel-ico">${a.nome.endsWith(".jpg") ? "▣" : "▤"}</span><span><b>${esc(a.nome)}</b><small>${a.tamanho} · ontem</small></span></button>`).join("")}
      </div>
    </div>`,
});

const PASSOS = [["inicio", "Escolher o arquivo"], ["conferindo", "Conferir se é o jogo certo"], ["preparando", "Preparar pro port (só da primeira vez)"], ["pronto", "Pronto pra jogar"]];
function desenharPassos() {
  const ordem = { inicio: 0, errado: 1, conferindo: 1, preparando: 2, pronto: 3 };
  const i = ordem[passo];
  tela.querySelector("[data-passos]").innerHTML = `<div class="lista-itens">${PASSOS.map(([, t], n) => {
    const tom = n < i || passo === "pronto" ? "ok" : n === i ? (passo === "errado" ? "warn" : "atual") : "depois";
    const s = tom === "ok" ? "✓" : tom === "warn" ? "!" : n + 1;
    return `<div class="item-linha ${tom}"><span class="s">${s}</span><span><b>${t}</b></span></div>`;
  }).join("")}</div>`;
}

function desenhar() {
  const f = tela.querySelector("[data-fluxo]");
  desenharPassos();
  if (passo === "inicio") {
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <h1>Falta o arquivo do jogo</h1>
      <p>O port usa o seu próprio jogo. Escolha o arquivo no aparelho e a gente confere se é o certo.</p>
      <p>Serve: <b>${esc(p.arquivo)}</b>.</p>
      <div class="acoes">
        <button type="button" class="btn primario" data-foco id="a-escolher" data-escolher>${g("a")}Escolher arquivo</button>
        <button type="button" class="btn contorno" data-foco id="a-onde" data-onde>Onde consigo isso?</button>
      </div>
      <p class="explica" data-explica hidden>Você precisa ter o jogo original. O arquivo vem de uma cópia que você mesmo fez do seu cartucho ou disco. O app não baixa nem distribui jogos.</p>`;
  } else if (passo === "conferindo") {
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <h1>Conferindo o arquivo…</h1>
      <div class="progresso"><span class="spinner"></span>${esc(escolhido?.nome ?? "zelda-oot-usa.z64")}</div>
      <p>Isso leva só uns segundos.</p>`;
    setTimeout(() => { passo = escolhido && !escolhido.serve ? "errado" : "preparando"; desenhar(); }, 1400);
  } else if (passo === "errado") {
    const a = escolhido ?? ARQUIVOS[1];
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <div class="resultado warn"><span class="icone">!</span><div><h1>Esse arquivo não serve</h1></div></div>
      <p>${esc(a.porque)}</p>
      <p>Serve: <b>${esc(p.arquivo)}</b>.</p>
      <div class="acoes">
        <button type="button" class="btn primario" data-foco id="a-outro" data-escolher>${g("a")}Escolher outro</button>
        <button type="button" class="btn contorno" data-foco id="a-tec" data-tecnico>${g("y")}Detalhes técnicos</button>
      </div>
      <div class="tecnico" data-tecnico-caixa>arquivo: ${esc(a.nome)}<br>identificado: ${esc(a.versao)}<br>sha1 encontrado: 1d2b3c…e9f0<br>sha1 aceitos: ad69c91157f6705e8ab06c79fe08aad47bb57ba7 (EUA 1.0), …</div>`;
  } else if (passo === "preparando") {
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <div class="resultado ok"><span class="icone">✓</span><div><h1>É esse mesmo</h1></div></div>
      <p>${esc((escolhido ?? ARQUIVOS[0]).versao)}.</p>
      <div class="progresso"><span class="spinner"></span>Preparando o jogo pro port<span class="barra"><i data-prep style="width:5%"></i></span></div>
      <p class="explica">Só acontece da primeira vez. Leva uns 30 segundos.</p>`;
    let x = 5; const t = setInterval(() => { x += 9; const el = tela.querySelector("[data-prep]"); if (el) el.style.width = `${Math.min(x, 100)}%`; if (x >= 100) { clearInterval(t); passo = "pronto"; desenhar(); } }, 160);
  } else if (passo === "pronto") {
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <div class="resultado ok"><span class="icone">✓</span><div><h1>Pronto pra jogar</h1></div></div>
      <p>O arquivo fica guardado. Você não precisa escolher de novo, nem depois de atualizar o port.</p>
      <div class="acoes">
        <button type="button" class="btn primario" data-foco id="a-jogar" data-jogar>${g("a")}Jogar</button>
        <button type="button" class="btn contorno" data-foco id="a-depois" data-depois>${g("b")}Depois</button>
      </div>`;
  }
  ligar();
}

function ligar() {
  const f = tela.querySelector("[data-fluxo]");
  f.querySelector("[data-escolher]")?.addEventListener("click", () => abrirCamada(seletor, seletor.querySelector("[data-arq]")));
  f.querySelector("[data-onde]")?.addEventListener("click", () => { f.querySelector("[data-explica]").hidden = false; });
  f.querySelector("[data-tecnico]")?.addEventListener("click", () => f.querySelector("[data-tecnico-caixa]").classList.toggle("aberto"));
  f.querySelector("[data-jogar]")?.addEventListener("click", () => irPara("abrindo", `?port=${p.id}`));
  f.querySelector("[data-depois]")?.addEventListener("click", () => voltar("inicio"));
  const primeiro = f.querySelector("[data-foco]");
  if (primeiro) focar(primeiro);
  const dicas = { inicio: [["a", "Escolher arquivo"], ["b", "Voltar"]], conferindo: [["b", "Cancelar"]], errado: [["a", "Escolher outro"], ["y", "Detalhes técnicos"], ["b", "Voltar"]], preparando: [], pronto: [["a", "Jogar"], ["b", "Depois"]] };
  trocarDicas(dicas[passo]);
}

const seletor = tela.querySelector("[data-seletor]");
seletor.querySelectorAll("[data-arq]").forEach((b) => b.addEventListener("click", () => {
  escolhido = ARQUIVOS[+b.dataset.arq];
  fecharCamada(seletor);
  passo = "conferindo";
  desenhar();
}));
tela.querySelector("[data-voltar]").addEventListener("click", () => voltar("detalhes"));

iniciarFoco({
  b: () => {
    if (camadaAberta() === seletor) return fecharCamada(seletor);
    if (passo === "pronto") return voltar("inicio");
    voltar("detalhes");
  },
  y: () => tela.querySelector("[data-tecnico]")?.click(),
});
desenhar();
montarRefinamento();
