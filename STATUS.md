# Status

*Atualizado em 2026-10-27.*

## Onde estamos

Esqueleto do app (issue #1) compilando: Início, Ports e Detalhes deitados, com foco por controle e um catálogo de exemplo. Selo: `AUTOMATED VERIFIED` (`:core:test` com 8 testes e `:app:assembleDebug` passando). Falta `TESTADO NO APARELHO`: instalar e navegar no AYN Odin.

## O que existe

- Os documentos do projeto (visão, decisões e arquitetura proposta).
- `laboratorio/`: 8 telas (Início, Ports, Detalhes, Ajustes, Arquivo do jogo, Instalar/atualizar, Abrindo/não abriu, Primeiro uso), deitadas (controle: foco, L1/R1, botões que seguem o controle em uso) e em pé (toque: abas embaixo, Opções que sobem de baixo).
- Painel de Refinamento do portfólio (D-006). R1 feita: o Victor aprovou 7 seções, todas na opção proposta.
- `android/`: projeto Gradle (`core` + `app`). Foco por controle (A, B, Y, L1/R1; Q/E/Y/Enter/Esc no teclado), botões na tela que seguem o controle (Sony = PlayStation) e somem com toque. Abre o port pelo pacote, se ele estiver instalado.

## Esperando o Victor

- Conectar o AYN Odin por `adb` pro teste do esqueleto.
- Conferir as escolhas da IA Q-a a Q-d e a stack D-005 (`DECISIONS.md`).

## Próximo

Testar a issue #1 no Odin. Depois: Opções (☰), versão em pé, e o catálogo de verdade (roadmap 4) com o pacote Android de cada port.
