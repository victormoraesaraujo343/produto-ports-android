# Decisões

## D-001 — Catálogo de ports, não emulador nem pacote próprio

**Status:** aceita · **Data:** 2026-10-26

**Contexto:** havia dois caminhos possíveis. Um era instalar os APKs oficiais de cada port; o outro, embutir os ports como bibliotecas num app só.

**Decisão:** o app instala e atualiza os APKs oficiais de cada port e abre cada um deles.

**Por quê:** não redistribuímos binário de ninguém, a licença fica com cada projeto e o trabalho é muito menor.

**Consequências:** não existe menu dentro do jogo. A volta pro launcher depende do Android (botão de voltar ou de recentes), e a configuração do arquivo do jogo muda de port pra port.

**Rever quando:** um port importante não tiver APK, ou a verificação de desenvolvedor do Google bloquear a instalação.

## D-002 — Visual do Hydra, comportamento a partir da pesquisa do Roseta

**Status:** aceita · **Data:** 2026-10-26

**Decisão:** o visual vem do Hydra: fundo `#121212`, Noto Sans, botões claros, raio de 8px, capas com sombra e banner grande. A organização e o comportamento vêm da pesquisa do Roseta (`roseta-ux/SYNTHESIS.md` e `roseta-design-system/PRINCIPLES.md`).

**Por quê:** o Victor pediu "a aparência do Hydra", e a pesquisa do Roseta já responde como um launcher pra portátil deve se comportar.

## D-003 — Um app que muda de jeito conforme a posição do celular

**Status:** aceita · **Data:** 2026-10-26

**Decisão:** deitado, o app funciona com controle: seções no topo trocadas com L1/R1, arte do jogo em foco como fundo e fileira de recentes. Em pé, funciona com o dedo: abas embaixo e lista. Nas duas posições, as seções são as mesmas (Início, Ports e Ajustes).

**Por quê:** o Victor escolheu juntar as opções A e C dos mockups (`laboratorio/_mockups/`). Manter as mesmas seções segue o princípio P-24 do Roseta, de que trocar de modo de uso muda a forma de interagir, não a estrutura.

## D-004 — Lab primeiro, mas pouco; depois o app

**Status:** aceita · **Data:** 2026-10-26

**Decisão:** as telas nascem no `laboratorio/` (HTML com o painel de Refinamento) em poucas rodadas. Depois vira app nativo.

**Por quê:** o Victor pediu um ritmo rápido, com pouca coisa no lab e logo partindo pro app.

## D-005 — App nativo em Kotlin + Jetpack Compose

**Status:** proposta (escolha da IA, o Victor ainda não opinou) · **Data:** 2026-10-26

**Por quê:** instalação de APK (`PackageInstaller`), acesso a arquivos (SAF) e entrada de controle são APIs nativas. O `roseta-launcher` já usa Kotlin + Compose, então dá pra reaproveitar padrões de lá.

## D-006 — Painel de Refinamento do portfólio

**Status:** aceita · **Data:** 2026-10-27

**Decisão:** o lab usa o painel do `victor-moraes-site` (layouts A/B/C, aprovar várias variações, arquivo de conversas), não o do `lab-modelo-loja`.

**Por quê:** o Victor pediu, porque considera o outro muito ruim.

**Consequências:** a variação troca com Shift + ←/→, já que as setas são o direcional do app. A página do sistema do painel não veio junto, porque depende da identidade do portfólio. Detalhes em `laboratorio/refinamento/README.md`.

## D-007 — Só o texto essencial

**Status:** aceita · **Data:** 2026-10-27

**Contexto:** no primeiro teste no Odin, o Victor achou o app "com texto de filler em extremo excesso".

**Decisão:** cada informação aparece uma vez e só onde é usada. O estado só aparece quando pede atenção, já que "Pronto" fica implícito no botão "Jogar". Explicação só existe onde o jogador precisa agir (ex.: "Serve: …"). As dicas de botões não repetem o que um botão grande já mostra. Detalhes técnicos (port, tipo, versão, projeto) ficam só na ficha de Detalhes.

**Consequências:** sai o nome sobre a capa em foco, que tinha sido aprovado na R1 (variação `recentesNome = foco`), porque repetia o título grande e o nome na arte. Se o Victor quiser de volta, é só reverter esse ponto. O lab ainda tem os textos antigos e vale como referência de layout, não de texto.

## D-008 — Ver os Detalhes antes de instalar

**Status:** aceita (pedido do Victor) · **Data:** 2026-10-27

**Decisão:** numa capa de port ainda não instalado, A abre os Detalhes, e o "Instalar" fica só lá dentro. Pra port instalado, A continua jogando direto.

**Por quê:** o Victor quer ver do que se trata (descrição, versão, tamanho, projeto) antes de baixar.

## D-009 — Detalhes com galeria de gameplay e fundo animado

**Status:** aceita (escolha do Victor no mockup `laboratorio/_mockups/03-galeria.html`: A + 1 + I2) · **Data:** 2026-10-27

**Decisão:**
- **Fundo (A):** as imagens da galeria vão passando (troca a cada ~6 s, com fade e zoom lento), atrás de um véu escuro.
- **Galeria em cima do nome** (mudança do Victor, que substitui o "1 + I2"): o visor 16:9 e as miniaturas **sem legenda** ficam no topo da coluna da esquerda, com o nome, o estado e os botões embaixo. ←/→ trocam a imagem e A abre em tela cheia.
- **Coluna da direita:** "O que o port precisa", "Sobre" e "Ficha", rolando.
- **Só gameplay, nesta ordem:** imagens do próprio port no repositório (README e pastas de imagens), depois as capturas do jogo original no libretro-thumbnails (todas as versões, sem repetidas) e por último a tela de título. A caixa não entra na galeria; ela já é a capa.

**Por quê:** o fundo dos Detalhes ficava vazio ou com uma imagem aleatória. O Victor pediu uma galeria, e com mais gameplay que capas.

**O que não deu:** clipe em vídeo. Não há fonte aberta de vídeo, e nenhum README verificado tinha vídeo ou GIF. Se um GIF aparecer, ele entra como imagem.

## Decisões da IA que o Victor precisa conferir

- **Q-a:** "Ports" substitui Catálogo e Biblioteca, com filtros Instalados, Disponíveis e Atualizações.
- **Q-b:** não existe aba Downloads; o progresso aparece na capa do próprio jogo.
- **Q-d:** em pé, tocar num jogo abre os detalhes; a ação principal fica num botão na própria linha (Início) ou na barra presa embaixo (Detalhes). Segurar o dedo abre as Opções, como atalho.
- **Q-c:** deitado, o direcional pra baixo abre a lista completa de ports, e os detalhes ficam no Y. Isso resolve aqui o conflito C-1 do Roseta.
