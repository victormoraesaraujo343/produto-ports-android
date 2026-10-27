/* Refinamento da Início. Painel: o do portfólio (../refinamento/), ligado por ../comum/lab.js.
   A primeira opção de cada variação é a proposta. */
import { criarLab } from "../comum/lab.js";

const SECOES = [
  { id: "topo", nome: "Seções no topo (L1/R1)" },
  { id: "destaque", nome: "Jogo em foco: arte, nome, estado e ação" },
  { id: "recentes", nome: "Fileira de recentes" },
  { id: "opcoes", nome: "Painel de Opções (☰ / ⋯)" },
  { id: "lista", nome: "Em pé: lista de recentes com ação em cada linha" },
  { id: "abas", nome: "Em pé: abas embaixo" },
];

const VARIANTES = {
  destaque: { secao: "destaque", rotulo: "Arte do jogo em foco", opcoes: [["fundo", "Fundo da tela inteira"], ["banner", "Banner com borda (como o hero do Hydra)"]] },
  recentesNome: { secao: "recentes", rotulo: "Nome nas capas", opcoes: [["foco", "Só na capa em foco"], ["todas", "Em todas"], ["nenhuma", "Nenhuma"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarLab({ pagina: "inicio", rotulo: "Início", secoes: SECOES, variantes: VARIANTES });
