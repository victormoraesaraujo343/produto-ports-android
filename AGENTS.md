# AGENTS.md — regras pra IA neste projeto

Baseado no [`sistema-ia`](https://github.com/victormoraesaraujo343/sistema-ia) (v2). O repositório é a fonte da verdade; o chat é só contexto.

## Ordem de autoridade

1. O que o Victor disse com todas as letras.
2. `DECISIONS.md`.
3. `ARCHITECTURE.md`.
4. `ROADMAP.md` e `STATUS.md`.
5. A issue em execução.
6. Chat e memória.

Contradição não se resolve em silêncio: aponte e registre a resolução.

## Esquema Lab + Desenvolvimento

- **Lab** (`laboratorio/`): as telas em HTML, cada uma nas versões deitada e em pé, com o painel de Refinamento **do portfólio** (tecla R, D-006). O Victor anota, coloca alfinetes, aprova e fecha a rodada. A IA lê `laboratorio/<tela>/ANOTACOES.md`, **traz uma síntese antes de executar**, executa, responde em `anotacoes.json` (`versoes.pagina.secoes[n]`) e faz um commit por rodada. O protocolo está em `laboratorio/refinamento/README.md`. Nunca rode teste que grave no `anotacoes.json` real.
- **Desenvolvimento** (`app/`, quando existir): o app Android em Kotlin + Compose implementa o que saiu do lab.
- O ritmo combinado é rápido: **poucas rodadas no lab** (D-004).
- Só o Victor aprova. "Ficou bom" não é aprovação. No painel, quem marca "Aprovada" é ele.

## Referências obrigatórias pra decisões de tela

- Visual: o Hydra (`src/renderer/src/scss/globals.scss` no fork `victormoraesaraujo343/hydra`).
- Comportamento: `roseta-ux/SYNTHESIS.md` e `roseta-design-system/PRINCIPLES.md`. Cite o número do princípio (P-xx) quando ele justificar uma escolha.

## Evidência

Separe fato, inferência, hipótese, decisão e recomendação. Use selos honestos: `LAB REVISADO`, `TESTADO NO APARELHO`, `PENDENTE`. Nada do lab vale como "funciona no Android".

## Relatório pro Victor

Curto: o que mudou, o que funciona, o que falta decidir ou testar. Sempre termine com **Resumo não-dev:** em uma ou duas frases.
