# Status

*Atualizado em 2026-10-27.*

## Onde estamos

O app funciona de ponta a ponta no AYN Odin 2 (`TESTADO NO APARELHO`, 2026-10-27). Ele lê a lista online do `ports-catalogue` (31 ports com APK, 28 sem os de VR) e instala o port oficial (Gen2Recomped) baixado do GitHub. O Android pediu a permissão e a confirmação, e o Victor aceitou. O **Jogar** abriu o port, que importou o dump do Victor (Pokémon Silver USA/Europe, SHA-1 conferido) e rodou o jogo. As capturas estão em `docs/aparelho/`. Capas reais do SteamGridDB conferidas no Odin, com a chave que o Victor colou em Ajustes.

## O que existe

- Os documentos do projeto (visão, decisões e arquitetura proposta).
- `laboratorio/`: 8 telas (Início, Ports, Detalhes, Ajustes, Arquivo do jogo, Instalar/atualizar, Abrindo/não abriu, Primeiro uso), deitadas (controle: foco, L1/R1, botões que seguem o controle em uso) e em pé (toque: abas embaixo, Opções que sobem de baixo).
- Painel de Refinamento do portfólio (D-006). R1 feita: o Victor aprovou 7 seções, todas na opção proposta.
- `android/`: projeto Gradle (`core` + `app`). Foco por controle (A, B, Y, L1/R1; Q/E/Y/Enter/Esc no teclado), botões na tela que seguem o controle (Sony = PlayStation) e somem com toque. Abre o port pelo pacote, se ele estiver instalado.

## Esperando o Victor

- Conferir as escolhas da IA Q-a a Q-d e a stack D-005 (`DECISIONS.md`).

## Próximo

Próximas issues (feitas por agentes Haiku, validadas pelo Opus): Opções (☰), versão em pé, e o catálogo de verdade (roadmap 4) com o pacote Android de cada port.
