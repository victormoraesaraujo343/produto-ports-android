import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "abrindo", nome: "Abrindo o jogo · aviso de como voltar" },
  { n: "caminhos", nome: "Não abriu: caminhos pra resolver" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "abrindo", secoes: SECOES, variantes: {} });
