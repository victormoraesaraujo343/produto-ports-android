import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "caminho", nome: "Caminho e voltar" },
  { id: "acao", nome: "Nome, estado e ação principal" },
  { id: "precisa", nome: "O que o port precisa (estado detalhado)" },
  { id: "info", nome: "Sobre, ficha e origem" },
  { id: "desinstalar", nome: "Confirmar desinstalação" },
  { id: "opcoes", nome: "Painel de Opções (☰)" },
];

const VARIANTES = {
  precisaMostrar: { secao: "precisa", rotulo: "Quando mostrar a lista", opcoes: [["sempre", "Sempre"], ["problema", "Só quando falta algo"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "detalhes", rotulo: "Detalhes do port", secoes: SECOES, variantes: VARIANTES });
