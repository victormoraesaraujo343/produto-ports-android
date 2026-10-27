import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "topo", nome: "Seções no topo (L1/R1)" },
  { n: "grupos", nome: "Grupos de ajustes" },
  { n: "itens", nome: "Ajustes de cada grupo" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "ajustes", secoes: SECOES, variantes: {} });
