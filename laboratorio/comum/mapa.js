/* O índice do lab pro menu "Páginas" do painel de Refinamento. */
const item = (pagina, rotulo, busca = "") => ({ id: pagina, rotulo, rota: `laboratorio/${pagina}/index.html${busca}`, pagina, versao: "pagina" });

export const MAPA = [
  { grupo: "Navegação", itens: [item("inicio", "Início"), item("ports", "Ports"), item("detalhes", "Detalhes do port"), item("ajustes", "Ajustes")] },
  { grupo: "Fluxos", itens: [item("arquivo", "Arquivo do jogo"), item("instalar", "Instalar e atualizar"), item("abrindo", "Abrindo e não abriu"), item("primeiro-uso", "Primeiro uso")] },
];
