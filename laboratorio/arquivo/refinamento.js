import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "caminho", nome: "Caminho e voltar" },
  { n: "fluxo", nome: "Texto e ação de cada passo (início, conferindo, não serve, preparando, pronto)" },
  { n: "passos", nome: "Lista de passos à direita" },
  { n: "seletor", nome: "Seletor de arquivos do Android (simulado)" },
];

const VARIANTES = {
  passosMostrar: { secao: "passos", rotulo: "Lista de passos", opcoes: [["mostrar", "Mostrar"], ["esconder", "Esconder"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "arquivo", secoes: SECOES, variantes: VARIANTES });
