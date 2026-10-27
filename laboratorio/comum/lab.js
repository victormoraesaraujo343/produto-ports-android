/* LAB — liga uma tela ao painel de Refinamento do portfólio.
   A tela declara as seções por nome ({ id, nome }); aqui elas viram números
   (o painel mostra o número de cada seção) e os elementos com data-lab="<id>"
   ganham data-secao="<n>", inclusive os que aparecem depois (fluxos que
   redesenham). Cada tela é uma página com uma versão só. */
import { criarRefinamento } from "../refinamento/painel.js";
import { MAPA } from "./mapa.js";

export function criarLab({ pagina, rotulo, secoes, variantes = {} }) {
  const S = Object.fromEntries(secoes.map((s, i) => [s.id, String(i + 1)]));
  const marcar = (raiz) => {
    if (raiz.nodeType !== 1 && raiz !== document) return;
    if (raiz.dataset?.lab && S[raiz.dataset.lab]) raiz.dataset.secao = S[raiz.dataset.lab];
    raiz.querySelectorAll("[data-lab]").forEach((el) => { if (S[el.dataset.lab]) el.dataset.secao = S[el.dataset.lab]; });
  };
  new MutationObserver((ms) => ms.forEach((m) => m.addedNodes.forEach(marcar))).observe(document.documentElement, { childList: true, subtree: true });
  marcar(document);

  const lab = criarRefinamento({
    pagina,
    secoes: secoes.map((s, i) => ({ n: String(i + 1), nome: s.nome })),
    versoes: [{ id: "pagina", rotulo, rota: `laboratorio/${pagina}/index.html`, disponivel: true }],
    variantes: Object.fromEntries(Object.entries(variantes).map(([k, v]) => [k, { ...v, secao: S[v.secao] ?? v.secao }])),
    mapa: MAPA,
  });
  return { ...lab, montar: () => { marcar(document); lab.montar(); } };
}
