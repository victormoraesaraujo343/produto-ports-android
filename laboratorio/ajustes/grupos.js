/* Os ajustes do app — os mesmos nas duas posições. */
export const GRUPOS = [
  ["atualizacoes", "Atualizações", [
    { id: "auto", nome: "Atualizar ports sozinho", valores: ["Só no Wi‑Fi", "Sempre", "Perguntar antes"], nota: "Atualiza em segundo plano os ports que este app instalou." },
    { id: "teste", nome: "Versões de teste", valores: ["Não", "Sim"], nota: "Algumas versões novas saem primeiro em teste e podem ter defeitos." },
    { id: "agora", nome: "Procurar agora", acao: "Última vez: hoje, 08:12" },
  ]],
  ["controles", "Controles e botões", [
    { id: "glifos", nome: "Botões mostrados na tela", valores: ["Auto (Xbox)", "Xbox", "PlayStation", "Esconder"], nota: "Auto segue o controle que você está usando." },
    { id: "confirmar", nome: "Botão de confirmar", valores: ["Auto (A embaixo)", "Embaixo", "À direita"], nota: "Pra controles estilo Nintendo, que trocam A e B de lugar." },
    { id: "vibrar", nome: "Vibração ao navegar", valores: ["Desligada", "Ligada"] },
  ]],
  ["arquivos", "Arquivos e espaço", [
    { id: "pasta", nome: "Pasta dos seus jogos", valores: ["Downloads/Jogos", "Escolher outra…"], nota: "O app procura aqui o arquivo que cada port precisa." },
    { id: "espaco", nome: "Espaço usado pelos ports", acao: "412 MB em 5 ports" },
    { id: "limpar", nome: "Apagar downloads que sobraram", acao: "38 MB" },
  ]],
  ["catalogo", "Catálogo", [
    { id: "fonte", nome: "De onde vem a lista", acao: "ports-catalogue (GitHub) ↗" },
    { id: "naotestados", nome: "Mostrar ports ainda não testados", valores: ["Sim", "Não"], nota: "Ports que entraram na lista sozinhos, sem ninguém testar." },
    { id: "naoroda", nome: "Mostrar ports que não rodam aqui", valores: ["Sim, marcados", "Não"] },
  ]],
  ["sobre", "Sobre", [
    { id: "versao", nome: "Versão do app", acao: "0.1 (lab)" },
    { id: "jogos", nome: "Este app não contém jogos", acao: "Cada port precisa do seu jogo original." },
    { id: "licencas", nome: "Licenças", acao: "Ver ↗" },
  ]],
];
