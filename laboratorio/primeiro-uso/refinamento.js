import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "passo", nome: "Cada passo: boas-vindas, permissão, pasta, pronto" },
  { n: "pontos", nome: "Indicador de passos" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "primeiro-uso", secoes: SECOES, variantes: {} });
