import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "topo", nome: "Seções no topo (L1/R1)" },
  { id: "filtros", nome: "Filtros, ordem e busca" },
  { id: "grade", nome: "Grade de capas" },
  { id: "contexto", nome: "Faixa de baixo: o port em foco" },
  { id: "busca", nome: "Busca com teclado na tela" },
  { id: "opcoes", nome: "Painel de Opções (☰ / toque longo)" },
  { id: "abas", nome: "Em pé: abas embaixo" },
];

const VARIANTES = {
  capas: { secao: "grade", rotulo: "Formato das capas", opcoes: [["vertical", "Vertical 3:4 (biblioteca do Hydra)"], ["horizontal", "Horizontal 16:9"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "ports", rotulo: "Ports", secoes: SECOES, variantes: VARIANTES });
