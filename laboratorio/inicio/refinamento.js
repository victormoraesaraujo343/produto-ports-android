/* Refinamento da Início. Motor genérico: ../refinamento/painel.js.
   A primeira opção de cada variação é a proposta. */
import { criarRefinamento } from "../refinamento/painel.js";

const SECOES = [
  { n: "topo", nome: "Seções no topo (L1/R1)" },
  { n: "destaque", nome: "Jogo em foco: arte, nome, estado e ação" },
  { n: "recentes", nome: "Fileira de recentes" },
  { n: "opcoes", nome: "Painel de Opções (☰)" },
];

const VARIANTES = {
  destaque: { secao: "destaque", rotulo: "Arte do jogo em foco", opcoes: [["fundo", "Fundo da tela inteira"], ["banner", "Banner com borda (como o hero do Hydra)"]] },
  recentesNome: { secao: "recentes", rotulo: "Nome nas capas", opcoes: [["foco", "Só na capa em foco"], ["todas", "Em todas"], ["nenhuma", "Nenhuma"]] },
};

export const { variante, aoMudar, montar: montarRefinamento } = criarRefinamento({ pagina: "inicio", secoes: SECOES, variantes: VARIANTES });
