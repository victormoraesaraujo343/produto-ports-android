# Status

*Atualizado em 2026-10-27.*

## Onde estamos

Esqueleto do app (issue #1) pronto e `TESTADO NO APARELHO` no AYN Odin 2 (Android 13, 1920×1080, 960×540 dp), em 2026-10-27. Funcionam: tela cheia, foco inicial, direcional, A, B, Y e L1/R1 entre Início, Ports e Detalhes. As capturas estão em `docs/aparelho/`. A conexão USB com o Odin cai com erro de protocolo; a depuração por Wi‑Fi funciona.

## O que existe

- Os documentos do projeto (visão, decisões e arquitetura proposta).
- `laboratorio/`: 8 telas (Início, Ports, Detalhes, Ajustes, Arquivo do jogo, Instalar/atualizar, Abrindo/não abriu, Primeiro uso), deitadas (controle: foco, L1/R1, botões que seguem o controle em uso) e em pé (toque: abas embaixo, Opções que sobem de baixo).
- Painel de Refinamento do portfólio (D-006). R1 feita: o Victor aprovou 7 seções, todas na opção proposta.
- `android/`: projeto Gradle (`core` + `app`). Foco por controle (A, B, Y, L1/R1; Q/E/Y/Enter/Esc no teclado), botões na tela que seguem o controle (Sony = PlayStation) e somem com toque. Abre o port pelo pacote, se ele estiver instalado.

## Esperando o Victor

- Conferir as escolhas da IA Q-a a Q-d e a stack D-005 (`DECISIONS.md`).

## Próximo

Próximas issues (feitas por agentes Haiku, validadas pelo Opus): Opções (☰), versão em pé, e o catálogo de verdade (roadmap 4) com o pacote Android de cada port.
