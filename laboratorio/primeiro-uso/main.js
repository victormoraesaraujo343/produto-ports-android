/* PRIMEIRO USO — deitado. O que nenhuma referência resolve (Síntese §5):
   permissão de instalar apps e onde estão os jogos. Pouco texto, uma decisão
   por vez, e o que é opcional diz que é opcional. */
import "./primeiro-uso.css";
import { PORTS } from "../comum/dados.js";
import { montarTela, esc, g, trocarDicas, irPara } from "../comum/tela.js";
import { iniciarFoco, focar } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

let passo = Number(new URLSearchParams(location.search).get("passo") ?? 0);
let permitido = false;
let pasta = null;

const PASSOS = [
  () => ({
    titulo: "Os seus jogos, nos ports da comunidade",
    texto: `<p>Este app instala os ports oficiais de cada projeto e abre o jogo pra você. Hoje são <b>${PORTS.length} ports</b> na lista.</p><p>Ele não vem com jogos: cada port usa o arquivo do seu jogo original.</p>`,
    acoes: [["a", "Começar", "proximo", "primario"]],
  }),
  () => ({
    titulo: "Permitir instalar os ports",
    texto: permitido
      ? `<div class="resultado ok"><span class="icone">✓</span><div><p><b>Permitido.</b> Na primeira instalação de cada port, o Android ainda pede uma confirmação.</p></div></div>`
      : `<p>Os ports não estão na Play Store. O Android precisa da sua permissão pra este app instalar e atualizar ports.</p><p class="explica">Vai abrir um ajuste do Android. Ligue "Permitir desta fonte" e volte.</p>`,
    acoes: permitido ? [["a", "Continuar", "proximo", "primario"]] : [["a", "Abrir ajuste do Android ↗", "permitir", "primario"], ["", "Agora não", "proximo", "contorno"]],
  }),
  () => ({
    titulo: "Onde estão os seus jogos?",
    texto: pasta
      ? `<div class="resultado ok"><span class="icone">✓</span><div><p><b>${esc(pasta)}</b>. Achamos 3 arquivos que servem pra ports da lista.</p></div></div>`
      : `<p>Se você já tem os arquivos dos jogos no aparelho, mostre a pasta e o app acha sozinho o que cada port precisa.</p><p class="explica">Opcional. Dá pra escolher jogo por jogo depois.</p>`,
    acoes: pasta ? [["a", "Continuar", "proximo", "primario"]] : [["a", "Escolher pasta", "pasta", "primario"], ["", "Pular", "proximo", "contorno"]],
  }),
  () => ({
    titulo: "Tudo pronto",
    texto: `<p>${pasta ? "Os ports que combinam com os seus jogos aparecem primeiro." : "Escolha um port pra começar."} O estado de cada um aparece na capa: <span class="estado ok"><span class="s">✓</span>pronto</span>, <span class="estado warn"><span class="s">!</span>falta algo</span>, <span class="estado neutro"><span class="s">+</span>não instalado</span>.</p>`,
    acoes: [["a", "Ver os ports", "fim", "primario"]],
  }),
];

const tela = montarTela({
  classe: "primeiro-uso",
  conteudo: `
    <div class="pu-mosaico" aria-hidden="true">${PORTS.filter((p) => p.cor).slice(0, 8).map((p) => `<span class="arte" style="--c1:${p.cor[0]};--c2:${p.cor[1]};--c3:${p.cor[2]}"></span>`).join("")}</div>
    <div class="pu-veu"></div>
    <section class="pu" data-lab="passo" data-pu></section>
    <div class="pu-pontos" data-lab="pontos" data-pontos></div>`,
});

function desenhar() {
  const d = PASSOS[passo]();
  tela.querySelector("[data-pu]").innerHTML = `
    <div class="sobre">${passo + 1} de ${PASSOS.length}</div>
    <h1>${d.titulo}</h1>
    ${d.texto}
    <div class="acoes">${d.acoes.map(([b, t, id, tipo]) => `<button type="button" class="btn ${tipo}" data-foco id="pu-${id}" data-acao="${id}">${b ? g(b) : ""}${esc(t)}</button>`).join("")}</div>`;
  tela.querySelector("[data-pontos]").innerHTML = PASSOS.map((_, i) => `<i class="${i === passo ? "atual" : i < passo ? "feito" : ""}"></i>`).join("");
  tela.querySelectorAll("[data-acao]").forEach((b) => b.addEventListener("click", () => ({
    proximo: () => { passo = Math.min(passo + 1, PASSOS.length - 1); desenhar(); },
    permitir: () => { permitido = true; desenhar(); },
    pasta: () => { pasta = "Downloads/Jogos"; desenhar(); },
    fim: () => irPara("ports", "?filtro=disponiveis"),
  })[b.dataset.acao]()));
  focar(tela.querySelector("[data-acao]"));
  trocarDicas(passo > 0 ? [["a", "Escolher"], ["b", "Voltar um passo"]] : [["a", "Começar"]]);
}

iniciarFoco({ b: () => { if (passo > 0) { passo--; desenhar(); } } });
desenhar();
montarRefinamento();
