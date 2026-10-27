/* FOCO — navegação por controle no lab, no espírito do PS4 (Síntese §7): o
   foco anda, o mundo fica parado. Funciona com teclado, controle de verdade
   (Gamepad API) e toque/mouse.

   Teclado: setas · Enter = A · Esc = B · X = X · Y = Y · Q/E = L1/R1 · O = ☰ Opções.
   Controle: layout padrão do navegador (A/✕ = 0, B/○ = 1, Y/△ = 3,
   L1 = 4, R1 = 5, ☰ = 9, direcional = 12–15, analógico esquerdo).

   Cada elemento focável tem `data-foco`. A camada ativa é a última com
   `data-camada` aberta (ex.: painel de Opções); fora dela nada recebe foco. */

const html = document.documentElement;
let atual = null;
let tratadores = {};

/* ------------------------------------------------------------ entrada em uso (P-18, P-23) */
export function definirEntrada(tipo) {
  if (html.dataset.entrada !== tipo) {
    html.dataset.entrada = tipo;
    try { sessionStorage.setItem("lab-entrada", tipo); } catch {}
  }
}
definirEntrada(sessionStorage.getItem("lab-entrada") ?? "xbox");

/* ------------------------------------------------------------ camadas */
const camadaAtiva = () => {
  const abertas = [...document.querySelectorAll("[data-camada].aberta")];
  return abertas.at(-1) ?? document.querySelector(".tela");
};
const focaveis = (raiz = camadaAtiva()) =>
  [...raiz.querySelectorAll("[data-foco]")].filter((el) => el.offsetParent !== null && !el.closest("[data-camada]:not(.aberta)"));

/* rola só o contêiner marcado com data-rolagem (scrollIntoView rolaria a
   própria .tela, que tem overflow hidden) */
function mostrar(el) {
  const caixa = el.parentElement?.closest("[data-rolagem]");
  if (!caixa) return;
  const c = caixa.getBoundingClientRect(), r = el.getBoundingClientRect();
  const folga = parseFloat(caixa.dataset.rolagem) || 24;
  let dx = 0, dy = 0;
  if (r.right > c.right - folga) dx = r.right - c.right + folga;
  else if (r.left < c.left + folga) dx = r.left - c.left - folga;
  if (r.bottom > c.bottom - folga) dy = r.bottom - c.bottom + folga;
  else if (r.top < c.top + folga) dy = r.top - c.top - folga;
  const z = parseFloat(getComputedStyle(document.querySelector(".tela")).zoom) || 1;
  if (dx || dy) caixa.scrollBy({ left: dx / z, top: dy / z, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
}

export function focar(el, { rolar = true } = {}) {
  if (!el || el === atual) return;
  atual?.classList.remove("focado");
  atual = el;
  el.classList.add("focado");
  if (rolar) mostrar(el);
  el.dispatchEvent(new CustomEvent("focado", { bubbles: true }));
  /* P-17: o foco sobrevive a sair e voltar */
  if (el.id) try { sessionStorage.setItem(`lab-foco-${location.pathname}`, el.id); } catch {}
}
export const focado = () => atual;

export function focoLembrado(padrao) {
  let id = null;
  try { id = sessionStorage.getItem(`lab-foco-${location.pathname}`); } catch {}
  return (id && document.getElementById(id)) || padrao;
}

/* ------------------------------------------------------------ movimento espacial */
function mover(dir) {
  const lista = focaveis().filter((el) => el !== atual);
  if (!atual || !camadaAtiva().contains(atual)) return focar(lista[0]);
  if (tratadores.mover?.(dir, atual) === false) return;
  const a = atual.getBoundingClientRect();
  const ax = a.left + a.width / 2, ay = a.top + a.height / 2;
  let melhor = null, nota = Infinity;
  for (const el of lista) {
    const r = el.getBoundingClientRect();
    const x = r.left + r.width / 2, y = r.top + r.height / 2;
    const dx = x - ax, dy = y - ay;
    const principal = { direita: dx, esquerda: -dx, baixo: dy, cima: -dy }[dir];
    if (principal <= 4) continue;
    const lateral = dir === "direita" || dir === "esquerda" ? Math.abs(dy) : Math.abs(dx);
    /* mesma linha/coluna pesa mais que distância: o eixo é previsível (Síntese §7) */
    const n = principal + lateral * 2.2;
    if (n < nota) { nota = n; melhor = el; }
  }
  if (melhor) focar(melhor);
  else tratadores.borda?.(dir, atual);
}

/* ------------------------------------------------------------ ações */
function apertar(botao) {
  if (botao === "a") return atual?.click();
  return tratadores[botao]?.(atual);
}

/* ------------------------------------------------------------ teclado */
const TECLAS = {
  ArrowUp: ["mover", "cima"], ArrowDown: ["mover", "baixo"], ArrowLeft: ["mover", "esquerda"], ArrowRight: ["mover", "direita"],
  Enter: ["botao", "a"], " ": ["botao", "a"], Escape: ["botao", "b"], Backspace: ["botao", "b"],
  x: ["botao", "x"], y: ["botao", "y"], q: ["botao", "l1"], e: ["botao", "r1"], o: ["botao", "menu"],
};
addEventListener("keydown", (ev) => {
  if (ev.target.closest?.(".rf-painel, .rf-pop, .rf-visor, .rf-botao, input, textarea, [contenteditable]")) return;
  if (ev.ctrlKey || ev.metaKey || ev.altKey || ev.shiftKey) return;
  const t = TECLAS[ev.key] ?? TECLAS[ev.key.toLowerCase?.()];
  if (!t) return;
  ev.preventDefault();
  /* a tecla é do app: o painel de Refinamento (registrado depois) não a vê */
  ev.stopImmediatePropagation();
  if (html.dataset.entrada === "toque") definirEntrada("xbox");
  t[0] === "mover" ? mover(t[1]) : apertar(t[1]);
});

/* ------------------------------------------------------------ toque e mouse
   Clicar foca e ativa. Só o toque troca a entrada pra "toque" (o mouse é a
   ferramenta do lab, não um modo do app). */
addEventListener("pointerdown", (ev) => {
  if (ev.target.closest?.(".rf-painel, .rf-pop, .rf-visor, .rf-botao")) return;
  if (ev.pointerType === "touch") definirEntrada("toque");
  const el = ev.target.closest?.("[data-foco]");
  if (el && camadaAtiva().contains(el)) focar(el, { rolar: false });
}, true);

/* ------------------------------------------------------------ controle de verdade */
const MAPA = { 0: "a", 1: "b", 2: "x", 3: "y", 4: "l1", 5: "r1", 9: "menu", 12: "cima", 13: "baixo", 14: "esquerda", 15: "direita" };
const DIRS = new Set(["cima", "baixo", "esquerda", "direita"]);
const antes = {};
let repete = { dir: null, em: 0 };
function ler() {
  for (const pad of navigator.getGamepads?.() ?? []) {
    if (!pad) continue;
    const ps = /054c|playstation|dualsense|dualshock/i.test(pad.id);
    let dirAgora = null;
    for (const [i, nome] of Object.entries(MAPA)) {
      const apertado = pad.buttons[i]?.pressed;
      if (DIRS.has(nome)) { if (apertado) dirAgora = nome; continue; }
      if (apertado && !antes[`${pad.index}-${i}`]) { definirEntrada(ps ? "ps" : "xbox"); apertar(nome); }
      antes[`${pad.index}-${i}`] = apertado;
    }
    const [x = 0, y = 0] = pad.axes;
    if (!dirAgora && Math.hypot(x, y) > 0.55) dirAgora = Math.abs(x) > Math.abs(y) ? (x > 0 ? "direita" : "esquerda") : (y > 0 ? "baixo" : "cima");
    const t = performance.now();
    if (dirAgora && dirAgora !== repete.dir) { definirEntrada(ps ? "ps" : "xbox"); mover(dirAgora); repete = { dir: dirAgora, em: t + 360 }; }
    else if (dirAgora && t > repete.em) { mover(dirAgora); repete.em = t + 110; }
    else if (!dirAgora && repete.dir) repete = { dir: null, em: 0 };
  }
  requestAnimationFrame(ler);
}
addEventListener("gamepadconnected", () => requestAnimationFrame(ler), { once: true });

/* ------------------------------------------------------------ API */
export function iniciarFoco({ inicial, ...t } = {}) {
  tratadores = t;
  focar(focoLembrado(inicial ?? focaveis()[0]), { rolar: false });
}
export function trocarTratadores(t) { tratadores = { ...tratadores, ...t }; }

/* abrir/fechar uma camada guardando de onde o foco veio (P-40) */
export function abrirCamada(el, primeiro) {
  el._volta = atual;
  el.classList.add("aberta");
  focar(primeiro ?? el.querySelector("[data-foco]"), { rolar: false });
}
export function fecharCamada(el) {
  el.classList.remove("aberta");
  focar(el._volta, { rolar: false });
}
export const camadaAberta = () => document.querySelector("[data-camada].aberta");
