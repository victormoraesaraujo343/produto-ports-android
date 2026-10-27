import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "passo", nome: "Cada passo: boas-vindas, permissão, pasta, pronto" },
  { id: "pontos", nome: "Indicador de passos" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "primeiro-uso", rotulo: "Primeiro uso", secoes: SECOES, variantes: {} });
