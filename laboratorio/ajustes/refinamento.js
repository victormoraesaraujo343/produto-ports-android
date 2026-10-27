import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "topo", nome: "Seções no topo (L1/R1)" },
  { id: "grupos", nome: "Grupos de ajustes" },
  { id: "itens", nome: "Ajustes de cada grupo" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "ajustes", rotulo: "Ajustes", secoes: SECOES, variantes: {} });
