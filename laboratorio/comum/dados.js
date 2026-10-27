/* DADOS DO LAB — ports de exemplo, inspirados em entradas reais do
   ports-catalogue. Sem imagem de jogo: a "arte" é um gradiente com a cor do
   jogo, e um port fica sem arte de propósito (P-08: o caso sem artwork é de
   primeira classe). Estados possíveis (o que a Prontidão vai calcular):
     pronto · baixando · atualizar · falta-arquivo · arquivo-errado ·
     disponivel · nao-roda */

export const PORTS = [
  {
    id: "sm64", titulo: "Super Mario 64", logo: "SUPER MARIO 64", port: "sm64coopdx", tipo: "decompilado", plataforma: "Nintendo 64",
    versao: "1.3.2", tamanho: "92 MB", cor: ["#e0452f", "#5a1a12", "#1a0a08"], estado: "pronto", jogadoHa: "ontem",
    descricao: "Super Mario 64 rodando nativo, com multijogador online, widescreen e 60 fps.",
    arquivo: "Super Mario 64 (EUA)", repo: "coop-deluxe/sm64coopdx",
  },
  {
    id: "mania", titulo: "Sonic Mania", logo: "SONIC MANIA", port: "RSDKv5", tipo: "decompilado", plataforma: "PC / consoles",
    versao: "1.1.0 → 1.1.1", tamanho: "38 MB", cor: ["#1f6fe0", "#0b2a5c", "#060e1f"], estado: "baixando", progresso: 62, jogadoHa: "há 3 dias",
    descricao: "O motor do Sonic Mania refeito a partir do jogo original. Usa o arquivo Data.rsdk da sua cópia.",
    arquivo: "Data.rsdk", repo: "RSDKModding/RSDKv5-Decompilation",
  },
  {
    id: "oot", titulo: "Ocarina of Time", logo: "OCARINA OF TIME", port: "Ship of Harkinian", tipo: "decompilado", plataforma: "Nintendo 64",
    versao: "9.0.2", tamanho: "141 MB", cor: ["#3fae68", "#123a22", "#08140c"], estado: "falta-arquivo", jogadoHa: "nunca jogado",
    descricao: "Zelda: Ocarina of Time com melhorias modernas, texturas em alta e randomizer.",
    arquivo: "Ocarina of Time (EUA 1.0 ou 1.2, ou GameCube)", repo: "HarbourMasters/Shipwright",
  },
  {
    id: "mm", titulo: "Majora's Mask", logo: "MAJORA'S MASK", port: "Zelda64Recomp", tipo: "recompilado", plataforma: "Nintendo 64",
    versao: "1.2.1", tamanho: "64 MB", cor: ["#8a4bd6", "#2b1452", "#120a1f"], estado: "atualizar", novaVersao: "1.2.2", jogadoHa: "semana passada",
    descricao: "Majora's Mask recompilado: 60 fps ou mais, widescreen e controles modernos.",
    arquivo: "Majora's Mask (EUA)", repo: "Zelda64Recomp/Zelda64Recomp",
  },
  {
    id: "pd", titulo: "Perfect Dark", logo: "PERFECT DARK", port: "perfect_dark", tipo: "decompilado", plataforma: "Nintendo 64",
    versao: "—", tamanho: "29 MB", cor: null, estado: "disponivel", jogadoHa: null,
    descricao: "Port nativo de Perfect Dark com mira por mouse e controle, alta resolução e 60 fps.",
    arquivo: "Perfect Dark (EUA 1.1)", repo: "fgsfdsfgs/perfect_dark",
  },
  {
    id: "unleashed", titulo: "Sonic Unleashed", logo: "SONIC UNLEASHED", port: "UnleashedRecomp", tipo: "recompilado", plataforma: "Xbox 360",
    versao: "—", tamanho: "180 MB", cor: ["#f0a020", "#5a3205", "#1a0f02"], estado: "nao-roda", jogadoHa: null,
    descricao: "Sonic Unleashed recompilado do Xbox 360.",
    motivo: "Precisa de Vulkan 1.3 e de uma GPU mais forte que a deste aparelho.",
    arquivo: "Sonic Unleashed (Xbox 360)", repo: "hedge-dev/UnleashedRecomp",
  },
  {
    id: "zelda3", titulo: "A Link to the Past", logo: "A LINK TO THE PAST", port: "zelda3", tipo: "decompilado", plataforma: "Super Nintendo",
    versao: "0.3", tamanho: "6 MB", cor: ["#c9a227", "#4a3a0a", "#141004"], estado: "pronto", jogadoHa: "há 2 semanas",
    descricao: "A Link to the Past reimplementado em C, com widescreen e melhorias opcionais.",
    arquivo: "Zelda 3 (EUA)", repo: "snesrev/zelda3",
  },
  {
    id: "sm", titulo: "Super Metroid", logo: "SUPER METROID", port: "sm", tipo: "decompilado", plataforma: "Super Nintendo",
    versao: "—", tamanho: "5 MB", cor: ["#d2562b", "#3d1608", "#120603"], estado: "disponivel", jogadoHa: null,
    descricao: "Super Metroid reimplementado em C.",
    arquivo: "Super Metroid (EUA/Japão)", repo: "snesrev/sm",
  },
  {
    id: "sf64", titulo: "Star Fox 64", logo: "STAR FOX 64", port: "Starship", tipo: "decompilado", plataforma: "Nintendo 64",
    versao: "—", tamanho: "88 MB", cor: ["#2c8fd6", "#0b2b45", "#040e17"], estado: "disponivel", jogadoHa: null,
    descricao: "Star Fox 64 nativo, com widescreen e altas taxas de quadros.",
    arquivo: "Star Fox 64 (EUA 1.1)", repo: "HarbourMasters/Starship",
  },
  {
    id: "mk64", titulo: "Mario Kart 64", logo: "MARIO KART 64", port: "SpaghettiKart", tipo: "decompilado", plataforma: "Nintendo 64",
    versao: "—", tamanho: "74 MB", cor: ["#e23b3b", "#4b0f0f", "#160404"], estado: "disponivel", jogadoHa: null,
    descricao: "Mario Kart 64 nativo, com pistas customizadas.",
    arquivo: "Mario Kart 64 (EUA)", repo: "HarbourMasters/SpaghettiKart",
  },
];

/* Recentes: só o que já foi jogado ou está em andamento, em ordem de uso.
   A Início é atalho; a biblioteca completa é "Ports" (Síntese §1). */
export const RECENTES = ["sm64", "mania", "oot", "mm", "zelda3"];

export const porId = (id) => PORTS.find((p) => p.id === id);

/* As palavras de cada estado — sem ROM, hash, OTR ou APK (VISION, anti-metas). */
export const ESTADOS = {
  "pronto":         { tom: "ok",   simbolo: "✓", curto: "Pronto",            longo: "Pronto pra jogar",              acao: "Jogar" },
  "baixando":       { tom: "dl",   simbolo: "↓", curto: "Baixando",          longo: "Baixando a atualização",       acao: "Jogar mesmo assim" },
  "atualizar":      { tom: "dl",   simbolo: "↑", curto: "Atualização",       longo: "Tem versão nova",               acao: "Jogar" },
  "falta-arquivo":  { tom: "warn", simbolo: "!", curto: "Falta o jogo",      longo: "Falta o arquivo do jogo",       acao: "Escolher arquivo" },
  "arquivo-errado": { tom: "warn", simbolo: "!", curto: "Arquivo não serve", longo: "Esse arquivo não serve",        acao: "Escolher outro" },
  "disponivel":     { tom: "neutro", simbolo: "+", curto: "Não instalado",   longo: "Ainda não instalado",           acao: "Instalar" },
  "nao-roda":       { tom: "bad",  simbolo: "×", curto: "Não roda aqui",     longo: "Não roda neste aparelho",       acao: "Ver por quê" },
};
