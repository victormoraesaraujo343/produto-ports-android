# Visão

## O que é

Um launcher Android que junta num lugar só os ports de jogos recompilados e decompilados (Ship of Harkinian, Zelda64Recomp, sm64coopdx, RSDK…). Ele instala e atualiza o APK oficial de cada port, ajuda o jogador a colocar o arquivo do jogo dele e abre o jogo. A aparência é a do Hydra, e o uso foi pensado pra controle e pra toque ao mesmo tempo.

É o irmão Android da aba Ports do fork do Hydra. Os dois leem o mesmo catálogo, o [`ports-catalogue`](https://github.com/victormoraesaraujo343/ports-catalogue).

## Por quê

No PC, o fork do Hydra já junta os ports. No Android, cada port é um APK solto no GitHub, e cada um tem seu próprio jeito de receber o arquivo do jogo. Hoje o jogador precisa caçar a release, instalar o arquivo por fora, descobrir qual versão da ROM serve e lembrar de atualizar.

## Como fica quando dá certo

```
Abrir o app → escolher o jogo → Jogar
```

Se faltar alguma coisa, o próprio botão diz o que falta e leva até a solução.

## Metas

- Uma biblioteca de ports com o visual do Hydra, usável deitado com controle e em pé com o dedo.
- O estado de cada port aparece no próprio jogo, antes do clique: pronto, baixando, atualizar ou falta o arquivo.
- Instalar e atualizar sem precisar sair do app pra procurar APK.
- Conferir o arquivo do jogo e dizer em palavras simples se ele serve.

## Fora da meta (por enquanto)

- Distribuir dados de jogo ou ROMs. Isso nunca vai acontecer.
- Recompilar ou empacotar os ports nós mesmos. O app usa os APKs oficiais de cada projeto.
- Loja, social, conquistas ou notícias.
- Publicar na Play Store, porque as regras dela não permitem instalar APKs de fora.

## Anti-metas (se acontecer, falhou mesmo funcionando)

- Um "Jogar" confiante que falha depois do clique.
- Termos técnicos (ROM, hash, OTR, APK) na tela padrão.
- Ficar parecendo o Obtainium, um gerenciador de APKs, em vez de um lugar pra jogar.
