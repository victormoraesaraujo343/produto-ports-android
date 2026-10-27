# Refinamento

Painel de revisão copiado do laboratório do portfólio (`victor-moraes-site/laboratorio/refinamento` e `laboratorio/sistema/`). O Victor preferiu este ao painel do `lab-modelo-loja`.

O que mudou em relação ao portfólio:

- A variação da seção na tela troca com **Shift + ←/→**. As setas sozinhas são o direcional do app (`comum/foco.js`).
- O servidor é um plugin do Vite (`../vite.config.mjs`) com os mesmos endpoints da `integracao.mjs` do portfólio: `/__lab/anotacoes`, `/__lab/resumo` e `/__lab/git`. `/indice/` e `/lab/sistema/` levam ao índice do lab; a página do sistema não foi copiada, porque depende da identidade do portfólio.
- Cada tela é uma página com uma versão só (`pagina`). As seções são declaradas em `<tela>/refinamento.js` por nome; `comum/lab.js` numera essas seções e marca os elementos `data-lab="nome"` com `data-secao="n"`.

Protocolo da IA (igual ao do portfólio): ler `laboratorio/<tela>/ANOTACOES.md`, trazer uma síntese ao Victor antes de executar, executar, responder em `anotacoes.json` (`versoes.pagina.secoes[n].resposta = { texto, rodada, em }` e `status = "pronta"`; ao terminar, `pedidoDeRevisao.atendido = true` e `atualizado`) e commitar a rodada. Nunca rodar teste que grave no `anotacoes.json` real.
