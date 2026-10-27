# Arquitetura (proposta)

> Ainda é proposta. Nada disso foi construído. O app nativo começa depois do lab (D-004).

## Peças

| Peça | O que faz |
|---|---|
| **Catálogo** | Lê o `ports-catalogue.json` (o mesmo do Hydra), guarda uma cópia local e atualiza uma vez por dia. Usa só as entradas que têm build Android. |
| **Instalador** | Pega a última release do port no GitHub, baixa o APK que bate com `assets.android.pattern`, confere a assinatura e instala pelo `PackageInstaller`. A primeira instalação pede um toque do jogador; do Android 12 em diante, as atualizações podem ser silenciosas. |
| **Prontidão** | Calcula o estado de cada port a partir de cinco perguntas: o APK está instalado? Na versão atual? O arquivo do jogo foi escolhido? O arquivo confere (hash)? O aparelho aguenta o port? O resultado vira as palavras da tela, nunca termos técnicos. |
| **Arquivo do jogo** | O jogador escolhe o arquivo pelo seletor do Android (SAF). O app confere o hash com os `requiredFiles` do catálogo e entrega ao port do jeito que aquele port aceita. Cada port declara esse jeito no catálogo: abrir com o arquivo, copiar pra uma pasta ou o port pede por conta própria. |
| **Lançador** | Abre o port pelo intent de launch do pacote. Ao voltar, o launcher retoma com o foco no mesmo jogo. |
| **Entrada** | Detecta se o jogador está usando toque, controle de Xbox ou de PlayStation e mostra os botões certos na tela (P-23 do Roseta). |

Não há servidor próprio. Tudo roda no aparelho e consulta o GitHub (releases) e o `ports-catalogue`.

## Mudanças necessárias no `ports-catalogue`

- Novo `assets.android = { pattern, package }`, onde `package` é o nome do pacote do APK, usado pra saber se está instalado e pra abrir o port.
- Novo `game.delivery`, que diz como o port recebe o arquivo do jogo: `open-with`, `copy-to-folder` ou `port-asks`.
- Curar à mão as primeiras entradas com APK (hoje só 6 das 259 mencionam Android).

## Riscos

- **Verificação de desenvolvedor do Google:** estava prevista pra começar no Brasil em setembro de 2026 e pode exigir instalação por um fluxo avançado ou por ADB. Precisa checar o estado atual.
- **Cada port recebe o arquivo do jogo de um jeito diferente.** Isso precisa ser levantado port a port.
- **Não existe volta garantida de dentro do jogo pro launcher** (D-001).

## Stack

Kotlin + Jetpack Compose (D-005). Os módulos seguem o modelo do `roseta-launcher`: `app`, `core` (prontidão, regras), `data` (catálogo, GitHub) e `installer`.
