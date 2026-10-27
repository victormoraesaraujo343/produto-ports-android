import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "topo", nome: "Seções no topo (L1/R1)" },
  { n: "filtros", nome: "Filtros, ordem e busca" },
  { n: "grade", nome: "Grade de capas" },
  { n: "contexto", nome: "Faixa de baixo: o port em foco" },
  { n: "busca", nome: "Busca com teclado na tela" },
  { n: "opcoes", nome: "Painel de Opções (☰)" },
];

const VARIANTES = {
  capas: { secao: "grade", rotulo: "Formato das capas", opcoes: [["vertical", "Vertical 3:4 (biblioteca do Hydra)"], ["horizontal", "Horizontal 16:9"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "ports", secoes: SECOES, variantes: VARIANTES });
