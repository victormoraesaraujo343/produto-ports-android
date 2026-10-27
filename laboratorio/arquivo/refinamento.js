import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "caminho", nome: "Caminho e voltar" },
  { id: "fluxo", nome: "Texto e ação de cada passo (início, conferindo, não serve, preparando, pronto)" },
  { id: "passos", nome: "Lista de passos à direita" },
  { id: "seletor", nome: "Seletor de arquivos do Android (simulado)" },
];

const VARIANTES = {
  passosMostrar: { secao: "passos", rotulo: "Lista de passos", opcoes: [["mostrar", "Mostrar"], ["esconder", "Esconder"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "arquivo", rotulo: "Arquivo do jogo", secoes: SECOES, variantes: VARIANTES });
