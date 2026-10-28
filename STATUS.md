# Status

*Atualizado em 2026-10-27.*

## Onde estamos

**v0.2.0 publicada** (release no GitHub) e `TESTADO NO APARELHO` no Odin 2 em 2026-10-27. A lista online do `ports-catalogue` tem 194 ports com APK Android (eram 31), vindos da awesome-decomp-games (incluindo os feitos com IA, com etiqueta) e de buscas automáticas no GitHub: em inglês, português e espanhol, forks Android e outros ports do mesmo autor. Passou no gabarito do Naruto (`docs/validacao/GABARITO-NARUTO.md`). Detalhes com galeria de gameplay em cima do nome (D-009), filtro por console (incluindo PC e Outros), Ver os Detalhes antes de instalar (D-008).

**Issue #8 feita** (D-010), `TESTADO NO APARELHO` no Odin em 2026-10-27: a lista mostra 180 jogos (191 ports), Perfect Dark com 3 fontes e Mario Kart Wii com 2, a janela de fontes no Instalar e o "Trocar fonte" no jogo instalado. Também corrigido: o ↓ do controle parava na terceira fileira da lista de Ports (a capa em foco subia 3 dp no layout e as vizinhas pareciam "embaixo" dela).

## O que existe

- Os documentos do projeto (visão, decisões e arquitetura proposta).
- `laboratorio/`: 8 telas (Início, Ports, Detalhes, Ajustes, Arquivo do jogo, Instalar/atualizar, Abrindo/não abriu, Primeiro uso), deitadas (controle: foco, L1/R1, botões que seguem o controle em uso) e em pé (toque: abas embaixo, Opções que sobem de baixo).
- Painel de Refinamento do portfólio (D-006). R1 feita: o Victor aprovou 7 seções, todas na opção proposta.
- `android/`: projeto Gradle (`core` + `app`). Foco por controle (A, B, Y, L1/R1; Q/E/Y/Enter/Esc no teclado), botões na tela que seguem o controle (Sony = PlayStation) e somem com toque. Abre o port pelo pacote, se ele estiver instalado.

## Esperando o Victor

- Conferir as escolhas da IA Q-a a Q-d e a stack D-005 (`DECISIONS.md`).

## Próximo

Próximas issues (feitas por agentes Haiku, validadas pelo Opus): Opções (☰), versão em pé, e o catálogo de verdade (roadmap 4) com o pacote Android de cada port.
