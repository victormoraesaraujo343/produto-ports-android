import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "caminho", nome: "Caminho e voltar" },
  { id: "fluxo", nome: "Baixando, conferindo, instalado / atualizado" },
  { id: "passos", nome: "Lista de passos à direita" },
  { id: "sistema", nome: "Confirmação do Android (simulada)" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "instalar", rotulo: "Instalar e atualizar", secoes: SECOES, variantes: {} });
