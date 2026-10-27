/* ABRINDO / NÃO ABRIU — deitado. Abrir é entregar o controle pro APK do port
   (D-001): não existe menu dentro do jogo. A tela avisa como voltar.
   Quando falha, não é um "OK" sozinho (Síntese §6, Q-15): diz o que
   aconteceu e oferece caminhos, do mais simples ao mais técnico. */
import "./abrindo.css";
import { porId } from "../comum/dados.js";
import { montarTela, esc, g, trocarDicas, irPara, voltar } from "../comum/tela.js";
import { iniciarFoco, focar } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

const params = new URLSearchParams(location.search);
const p = porId(params.get("port") ?? "sm64");
let etapa = params.get("etapa") ?? "abrindo";

const tela = montarTela({
  classe: "abrindo",
  conteudo: `
    <div class="ab-arte ${p.cor ? "arte" : ""}" ${p.cor ? `style="--c1:${p.cor[0]};--c2:${p.cor[1]};--c3:${p.cor[2]}"` : ""}></div>
    <div class="ab-veu"></div>
    <section class="ab-centro" data-lab="abrindo" data-centro></section>`,
});

function desenhar() {
  const c = tela.querySelector("[data-centro]");
  if (etapa === "abrindo") {
    c.className = "ab-centro";
    c.innerHTML = `
      <div class="logo">${esc(p.logo)}</div>
      <div class="ab-linha"><span class="spinner"></span>Abrindo ${esc(p.port)}…</div>
      <p class="ab-volta">Pra voltar ao launcher: botão <b>voltar</b> ou <b>início</b> do Android.</p>`;
    trocarDicas([]);
    setTimeout(() => { if (etapa === "abrindo") { etapa = "simulacao"; desenhar(); } }, 1700);
  } else if (etapa === "simulacao") {
    c.className = "ab-centro";
    c.innerHTML = `
      <div class="lab-aviso">
        <b>No app de verdade, o jogo abre aqui.</b>
        <span>Simular o que acontece depois:</span>
        <div class="acoes">
          <button type="button" class="btn primario" data-foco id="s-volta" data-sim="volta">Voltei do jogo</button>
          <button type="button" class="btn contorno" data-foco id="s-falha" data-sim="falha">O jogo fechou sozinho</button>
        </div>
      </div>`;
    c.querySelector('[data-sim="volta"]').onclick = () => irPara("inicio");
    c.querySelector('[data-sim="falha"]').onclick = () => { etapa = "falha"; desenhar(); };
    focar(c.querySelector("[data-foco]"));
    trocarDicas([["a", "Escolher"]]);
  } else if (etapa === "falha") {
    c.className = "ab-centro falha";
    c.innerHTML = `
      <div class="resultado bad"><span class="icone">!</span><div>
        <h1>${esc(p.titulo)} fechou logo depois de abrir</h1>
        <p>Isso aconteceu dentro do port, não no launcher. O seu arquivo do jogo e o progresso salvo estão intactos.</p>
      </div></div>
      <div class="caminhos" data-lab="caminhos">
        <button type="button" class="caminho-item" data-foco id="f-tentar" data-c="tentar"><b>Tentar de novo</b><small>Às vezes é só isso.</small></button>
        <button type="button" class="caminho-item" data-foco id="f-ajustes" data-c="ajustes"><b>Abrir os ajustes do port ↗</b><small>Baixar a resolução costuma resolver em aparelho mais fraco.</small></button>
        <button type="button" class="caminho-item" data-foco id="f-reinstalar" data-c="reinstalar"><b>Reinstalar o port</b><small>Baixa de novo. O arquivo do jogo continua guardado.</small></button>
        <button type="button" class="caminho-item" data-foco id="f-versao" data-c="versao"><b>Voltar pra versão anterior</b><small>${esc(p.versao.split(" ")[0])} saiu há 2 dias. A anterior abria neste aparelho.</small></button>
        <button type="button" class="caminho-item" data-foco id="f-relatorio" data-c="relatorio"><b>Ver o relatório</b><small>O que o Android registrou, pra mandar pro projeto.</small></button>
      </div>
      <pre class="relatorio" data-relatorio hidden>FATAL EXCEPTION: main
Process: com.coopdx.sm64, PID: 18204
java.lang.UnsatisfiedLinkError: dlopen failed: library "libGLESv3.so"…
vulkan: VK_ERROR_INCOMPATIBLE_DRIVER</pre>`;
    c.querySelectorAll("[data-c]").forEach((b) => (b.onclick = () => ({
      tentar: () => { etapa = "abrindo"; desenhar(); },
      reinstalar: () => irPara("instalar", `?port=${p.id}&modo=atualizar`),
      versao: () => irPara("instalar", `?port=${p.id}&modo=atualizar`),
      relatorio: () => { c.querySelector("[data-relatorio]").hidden = false; },
      ajustes: () => {},
    })[b.dataset.c]()));
    focar(c.querySelector("[data-foco]"));
    trocarDicas([["a", "Escolher"], ["b", "Voltar"]]);
  }
}

iniciarFoco({ b: () => voltar("inicio") });
desenhar();
montarRefinamento();
