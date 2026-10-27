/* INSTALAR / ATUALIZAR — deitado. Baixa o APK oficial do GitHub, confere e
   instala. Na primeira instalação o Android pede confirmação (diálogo do
   sistema, simulado); atualização pode ser silenciosa (Android 12+). O
   progresso também aparece na capa do jogo (Síntese §5), então dá pra sair
   daqui e continuar navegando. */
import { porId } from "../comum/dados.js";
import { montarTela, esc, g, trocarDicas, irPara, voltar } from "../comum/tela.js";
import { iniciarFoco, focar, abrirCamada, fecharCamada, camadaAberta } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";
import "./instalar.css";

const params = new URLSearchParams(location.search);
const p = porId(params.get("port") ?? "pd");
const atualizando = params.get("modo") === "atualizar";
let passo = params.get("passo") ?? "baixando";

const tela = montarTela({
  classe: "instalar",
  caminho: [p.titulo, atualizando ? "Atualizar" : "Instalar"],
  conteudo: `
    <div class="fluxo-arte ${p.cor ? "arte" : ""}" ${p.cor ? `style="--c1:${p.cor[0]};--c2:${p.cor[1]};--c3:${p.cor[2]}"` : ""}></div>
    <div class="fluxo-veu"></div>
    <section class="fluxo" data-lab="fluxo" data-fluxo></section>
    <aside class="passos" data-lab="passos" data-passos></aside>
    <div class="sistema-fundo" data-camada data-sistema data-lab="sistema">
      <div class="sistema" role="dialog" aria-label="Diálogo do Android">
        <div class="sis-ico">${esc(p.titulo[0])}</div>
        <b>${esc(p.titulo)}</b>
        <p>Deseja instalar este app?</p>
        <div class="sis-acoes"><button type="button" data-foco data-sis-cancelar>Cancelar</button><button type="button" data-foco data-sis-instalar>Instalar</button></div>
      </div>
      <p class="sis-nota">Diálogo do Android (simulado). Ele aparece só na primeira instalação de cada port.</p>
    </div>`,
});

const PASSOS = atualizando
  ? [["baixando", "Baixar a versão nova"], ["conferindo", "Conferir o arquivo"], ["instalando", "Atualizar"], ["pronto", "Pronto"]]
  : [["baixando", "Baixar do GitHub"], ["conferindo", "Conferir o arquivo"], ["confirmar", "Confirmar no Android"], ["pronto", "Instalado"]];

function desenharPassos() {
  const i = PASSOS.findIndex(([id]) => id === passo || (passo === "instalando" && id === "confirmar"));
  tela.querySelector("[data-passos]").innerHTML = `<div class="lista-itens">${PASSOS.map(([, t], n) => {
    const tom = n < i || passo === "pronto" ? "ok" : n === i ? "atual" : "depois";
    return `<div class="item-linha ${tom}"><span class="s">${tom === "ok" ? "✓" : n === i ? '<span class="spinner mini"></span>' : n + 1}</span><span><b>${t}</b></span></div>`;
  }).join("")}</div>`;
}

let timer;
function desenhar() {
  clearInterval(timer);
  desenharPassos();
  const f = tela.querySelector("[data-fluxo]");
  const titulo = atualizando ? `Atualizando pra ${p.novaVersao ?? "1.2.2"}` : `Instalando · ${p.tamanho}`;
  if (passo === "baixando") {
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <h1>${titulo}</h1>
      <div class="progresso"><span class="barra"><i data-dl style="width:0%"></i></span><span data-dl-txt>0%</span></div>
      <p class="explica">Do projeto oficial no GitHub (${esc(p.repo)}). Pode sair daqui: o download continua e o progresso aparece na capa do jogo.</p>
      ${atualizando ? `<div class="novidades"><div class="rotulo">O que mudou</div><p>Correções de áudio, suporte a mais controles e melhorias de desempenho.</p></div>` : ""}
      <div class="acoes"><button type="button" class="btn contorno" data-foco id="i-sair" data-sair>${g("b")}Continuar navegando</button></div>`;
    let x = 0;
    timer = setInterval(() => {
      x += 7;
      const el = tela.querySelector("[data-dl]"); if (!el) return clearInterval(timer);
      el.style.width = `${Math.min(x, 100)}%`;
      tela.querySelector("[data-dl-txt]").textContent = `${Math.min(x, 100)}%`;
      if (x >= 100) { passo = "conferindo"; desenhar(); }
    }, 140);
  } else if (passo === "conferindo") {
    f.innerHTML = `<div class="sobre">${esc(p.titulo)}</div><h1>Conferindo o arquivo…</h1><div class="progresso"><span class="spinner"></span>Assinatura do projeto</div>`;
    timer = setTimeout(() => { passo = atualizando ? "instalando" : "confirmar"; desenhar(); }, 1100);
  } else if (passo === "confirmar") {
    f.innerHTML = `<div class="sobre">${esc(p.titulo)}</div><h1>Confirme no Android</h1><p>O Android pede pra confirmar a instalação de apps de fora da loja.</p>`;
    abrirCamada(sistema, sistema.querySelector("[data-sis-instalar]"));
  } else if (passo === "instalando") {
    f.innerHTML = `<div class="sobre">${esc(p.titulo)}</div><h1>${atualizando ? "Atualizando…" : "Instalando…"}</h1><div class="progresso"><span class="spinner"></span>${atualizando ? "Sem pedir nada: o app já tem permissão pra atualizar os ports que instalou." : ""}</div>`;
    timer = setTimeout(() => { passo = "pronto"; desenhar(); }, 1200);
  } else if (passo === "cancelado") {
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <div class="resultado warn"><span class="icone">!</span><div><h1>Instalação cancelada</h1></div></div>
      <p>O arquivo já foi baixado. Pra instalar, é só confirmar no Android.</p>
      <div class="acoes"><button type="button" class="btn primario" data-foco id="i-de-novo" data-de-novo>${g("a")}Tentar de novo</button><button type="button" class="btn contorno" data-foco id="i-voltar" data-sair>${g("b")}Voltar</button></div>`;
  } else if (passo === "pronto") {
    const faltaArquivo = !atualizando;
    f.innerHTML = `
      <div class="sobre">${esc(p.titulo)}</div>
      <div class="resultado ok"><span class="icone">✓</span><div><h1>${atualizando ? `Atualizado pra ${esc(p.novaVersao ?? "1.2.2")}` : "Instalado"}</h1></div></div>
      ${faltaArquivo ? `<p>Agora falta só o arquivo do jogo: <b>${esc(p.arquivo)}</b>.</p>` : "<p>Seu progresso continua onde estava.</p>"}
      <div class="acoes">
        <button type="button" class="btn primario" data-foco id="i-prox" data-prox>${g("a")}${faltaArquivo ? "Escolher arquivo" : "Jogar"}</button>
        <button type="button" class="btn contorno" data-foco id="i-depois" data-sair>${g("b")}Depois</button>
      </div>`;
  }
  f.querySelector("[data-sair]")?.addEventListener("click", () => voltar("ports"));
  f.querySelector("[data-de-novo]")?.addEventListener("click", () => { passo = "confirmar"; desenhar(); });
  f.querySelector("[data-prox]")?.addEventListener("click", () => irPara(atualizando ? "abrindo" : "arquivo", `?port=${p.id}`));
  const primeiro = f.querySelector("[data-foco]");
  if (primeiro && !camadaAberta()) focar(primeiro);
  trocarDicas({ baixando: [["b", "Continuar navegando"]], conferindo: [], confirmar: [["a", "Escolher"]], instalando: [], cancelado: [["a", "Tentar de novo"], ["b", "Voltar"]], pronto: [["a", atualizando ? "Jogar" : "Escolher arquivo"], ["b", "Depois"]] }[passo]);
}

const sistema = tela.querySelector("[data-sistema]");
sistema.querySelector("[data-sis-instalar]").addEventListener("click", () => { fecharCamada(sistema); passo = "instalando"; desenhar(); });
sistema.querySelector("[data-sis-cancelar]").addEventListener("click", () => { fecharCamada(sistema); passo = "cancelado"; desenhar(); });
tela.querySelector("[data-voltar]").addEventListener("click", () => voltar("ports"));

iniciarFoco({
  b: () => {
    if (camadaAberta() === sistema) return sistema.querySelector("[data-sis-cancelar]").click();
    voltar("ports");
  },
});
desenhar();
montarRefinamento();
