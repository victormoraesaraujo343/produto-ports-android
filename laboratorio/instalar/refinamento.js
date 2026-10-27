import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "caminho", nome: "Caminho e voltar" },
  { n: "fluxo", nome: "Baixando, conferindo, instalado / atualizado" },
  { n: "passos", nome: "Lista de passos à direita" },
  { n: "sistema", nome: "Confirmação do Android (simulada)" },
];

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "instalar", secoes: SECOES, variantes: {} });
