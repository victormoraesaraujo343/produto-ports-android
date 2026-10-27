/* Sistema do laboratório · peças em HTML. O painel (refinamento/painel.js) e a página do
   sistema (/pt/lab/sistema/) montam os componentes com estas mesmas funções. O visual está
   em componentes.css; aqui só a marcação. */

export const esc = (s = "") => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);

/* ícones: 24 × 24, traço 1,6, cantos redondos. Desenhar novos no mesmo grid. */
export const ICONES = {
  pin: '<path d="M12 21s-6-5.3-6-11a6 6 0 0 1 12 0c0 5.7-6 11-6 11z"/><circle cx="12" cy="10" r="2.2"/>',
  mobile: '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
  numero: '<path d="M5 9h14M5 15h14M10 4 8 20M16 4l-2 16"/>',
  seguir: '<circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/>',
  lado: '<path d="M4 8h14l-3-3M20 16H6l3 3"/>',
  mini: '<path d="M6 12h12"/>',
  fechar: '<path d="M6 6l12 12M18 6 6 18"/>',
  ajuda: '<circle cx="12" cy="12" r="9"/><path d="M9.6 9.3a2.5 2.5 0 0 1 4.8.9c0 1.7-2.4 2-2.4 3.6"/><path d="M12 17.2h.01"/>',
  ia: '<path d="M12 3l1.8 4.7L18.5 9.5l-4.7 1.8L12 16l-1.8-4.7L5.5 9.5l4.7-1.8z"/>',
  paginas: '<rect x="4" y="4" width="16" height="16" rx="2"/><path d="M4 9h16M9 9v11"/>',
  nota: '<path d="M5 19h4L19.5 8.5a2.1 2.1 0 0 0-3-3L6 16z"/><path d="M14.5 7.5l2 2"/>',
  esq: '<path d="M15 6l-6 6 6 6"/>',
  dir: '<path d="M9 6l6 6-6 6"/>',
  cima: '<path d="M6 15l6-6 6 6"/>',
  baixo: '<path d="M6 9l6 6 6-6"/>',
  ok: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
  indice: '<path d="M8 6h12M8 12h12M8 18h12"/><path d="M4 6h.01M4 12h.01M4 18h.01"/>',
  arquivo: '<rect x="3" y="4" width="18" height="5" rx="1"/><path d="M5 9v10h14V9M10 13h4"/>',
  sistema: '<rect x="3.5" y="3.5" width="7" height="7" rx="1"/><rect x="13.5" y="3.5" width="7" height="7" rx="1"/><rect x="3.5" y="13.5" width="7" height="7" rx="1"/><circle cx="17" cy="17" r="3.5"/>',
};
export const NOMES_ICONES = {
  pin: "alfinete", mobile: "mobile", numero: "numerar", seguir: "acompanhar", lado: "trocar de lado", mini: "minimizar", fechar: "fechar",
  ajuda: "atalhos", ia: "IA respondeu", paginas: "páginas", nota: "anotação", esq: "anterior", dir: "próxima", cima: "abrir", baixo: "recolher",
  ok: "aprovada", indice: "índice", arquivo: "arquivo", sistema: "sistema",
};
export const ico = (n, { pequeno = false } = {}) => `<svg class="rf-ico${pequeno ? " p" : ""}" viewBox="0 0 24 24" aria-hidden="true">${ICONES[n]}</svg>`;

/* botão de ícone; `ligado` = aria-pressed (ferramenta ligada) */
export const botaoIcone = ({ acao, arg, icone, rotulo, atalho, ligado, classe = "" }) =>
  `<button type="button" class="rf-x${classe ? ` ${classe}` : ""}" data-acao="${acao}"${arg != null ? ` data-arg="${arg}"` : ""}${ligado != null ? ` aria-pressed="${!!ligado}"` : ""} title="${esc(rotulo)}${atalho ? `  ${atalho}` : ""}" aria-label="${esc(rotulo)}">${ico(icone)}</button>`;

/* segmentado: [[id, nome]]; `atual` = id escolhido */
export const segmentado = ({ opcoes, atual, acao, rotulo }) =>
  `<div class="rf-layouts" role="group" aria-label="${esc(rotulo)}">${opcoes.map(([id, nome]) => `<button type="button" data-acao="${acao}" data-arg="${id}" aria-pressed="${atual === id}" title="${esc(nome)}">${esc(id.toUpperCase())}</button>`).join("")}</div>`;

/* ponto de status; sem status = vazio (guarda o lugar) */
export const NOME_STATUS = { revisar: "Em aberto", pronta: "IA respondeu", aprovada: "Aprovada", ajustar: "Ajustar", descartar: "Descartada" };
export const ponto = (status) => (status ? `<span class="rf-ponto" data-status="${status}" title="${NOME_STATUS[status]}"></span>` : `<span class="rf-ponto rf-ponto-vazio"></span>`);

/* bloco de pedido/resposta: corta em 3 linhas e abre ao clicar */
export const bloco = (tipo, rotulo, texto) => `<div class="rf-bloco ${tipo}" data-expandir><span>${rotulo}</span><p>${esc(texto)}</p></div>`;

/* opção de variação; `compacto` mostra só a letra */
export const opcao = ({ chave, id, nome, dica, atual, compacto = false }) =>
  `<button type="button" data-var="${chave}" data-valor="${id}" aria-pressed="${!!atual}" title="${esc(dica ?? nome)}">${esc(compacto ? nome.split(" · ")[0] : nome)}</button>`;

/* barra de progresso: aprovadas + ajustar sobre o total */
export const progresso = ({ aprovadas, ajustar, total, extra = "" }) =>
  `<div class="rf-prog"><span><b>${aprovadas}</b>/${total} aprovadas${ajustar ? ` · ${ajustar} ajustar` : ""} · ${total - aprovadas - ajustar} em aberto${extra}</span><div class="rf-barra"><i style="width:${(aprovadas / total) * 100}%"></i><i class="ajustar" style="width:${(ajustar / total) * 100}%"></i></div></div>`;

export const tecla = (t) => `<kbd class="rf-tecla">${esc(t)}</kbd>`;
