import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "abrindo", nome: "Abrindo o jogo · aviso de como voltar" },
  { id: "caminhos", nome: "Não abriu: caminhos pra resolver" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "abrindo", rotulo: "Abrindo e não abriu", secoes: SECOES, variantes: {} });
