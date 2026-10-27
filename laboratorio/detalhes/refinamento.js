import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "caminho", nome: "Caminho e voltar" },
  { n: "acao", nome: "Nome, estado e ação principal" },
  { n: "precisa", nome: "O que o port precisa (estado detalhado)" },
  { n: "info", nome: "Sobre, ficha e origem" },
  { n: "desinstalar", nome: "Confirmar desinstalação" },
  { n: "opcoes", nome: "Painel de Opções (☰)" },
];

const VARIANTES = {
  precisaMostrar: { secao: "precisa", rotulo: "Quando mostrar a lista", opcoes: [["sempre", "Sempre"], ["problema", "Só quando falta algo"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "detalhes", secoes: SECOES, variantes: VARIANTES });
