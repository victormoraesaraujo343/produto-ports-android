/* DETALHES — deitado. A profundidade "sob demanda" do estado (P-30): o que a
   capa resume num símbolo aqui vira a lista do que o port precisa. A ação
   principal fica no mesmo lugar e muda as palavras (P-26). Informação que não
   é do app vem com a fonte (P-29). */
import "./detalhes.css";
import { porId } from "../comum/dados.js";
import { montarTela, arte, esc, g, estadoHtml, estadoDe, trocarDicas, irPara, voltar, montarOpcoes, abrirOpcoes, fecharOpcoes, executarOpcao, acaoPrincipal } from "../comum/tela.js";
import { iniciarFoco, abrirCamada, fecharCamada, camadaAberta } from "../comum/foco.js";
import { montarRefinamento } from "./refinamento.js";

const params = new URLSearchParams(location.search);
const p = porId(params.get("port") ?? "oot");
const e = estadoDe(p);
const instalado = !["disponivel", "nao-roda"].includes(p.estado);

/* as cinco perguntas da Prontidão (ARCHITECTURE.md), em palavras do jogador */
function checklist() {
  const itens = [];
  if (p.estado === "nao-roda") {
    itens.push(["bad", "×", "Não roda neste aparelho", p.motivo]);
    itens.push(["neutro", "+", "Port não instalado", `${p.tamanho} pra baixar`]);
    return itens;
  }
  itens.push(instalado ? ["ok", "✓", "Port instalado", `versão ${p.versao.split(" ")[0]}`] : ["neutro", "+", "Port não instalado", `${p.tamanho} pra baixar do GitHub`]);
  if (p.estado === "atualizar") itens.push(["dl", "↑", `Versão nova: ${p.novaVersao}`, "Dá pra jogar agora e atualizar depois"]);
  else if (p.estado === "baixando") itens.push(["dl", "↓", `Baixando a versão nova · ${p.progresso}%`, "Dá pra jogar a versão atual enquanto isso"]);
  else if (instalado) itens.push(["ok", "✓", "Na versão mais nova", ""]);
  if (p.estado === "falta-arquivo" || !instalado) itens.push(["warn", "!", "Falta o arquivo do jogo", `Serve: ${p.arquivo}`]);
  else itens.push(["ok", "✓", "Arquivo do jogo conferido", p.arquivo]);
  itens.push(["ok", "✓", "Roda neste aparelho", "Pelo que o catálogo pede"]);
  return itens;
}

const acaoTexto = p.estado === "nao-roda" ? "Instalar mesmo assim" : e.acao;
const tela = montarTela({
  classe: "detalhes",
  caminho: ["Ports", p.titulo],
  conteudo: `
    <div class="hero-arte ${p.cor ? "arte" : "sem-arte"}" ${p.cor ? `style="--c1:${p.cor[0]};--c2:${p.cor[1]};--c3:${p.cor[2]}"` : ""}></div>
    <div class="hero-veu"></div>
    <section class="coluna-acao" data-lab="acao">
      <div class="logo">${esc(p.logo)}</div>
      <div class="meta">${esc(p.port)} · ${esc(p.tipo)} · ${esc(p.plataforma)}</div>
      <div class="linha-estado">${estadoHtml(p)}</div>
      <div class="acoes">
        <button type="button" class="btn ${p.estado === "nao-roda" ? "contorno" : "primario"}" id="d-principal" data-foco data-principal>${g("a")}${esc(acaoTexto)}</button>
        <button type="button" class="btn contorno" id="d-opcoes" data-foco data-mais>${g("menu")}Opções</button>
      </div>
      <div class="precisa" data-lab="precisa">
        <div class="rotulo">O que o port precisa</div>
        <div class="lista-itens">
          ${checklist().map(([tom, s, t, d]) => `<div class="item-linha ${tom}"><span class="s">${s}</span><span><b>${esc(t)}</b>${d ? `<small>${esc(d)}</small>` : ""}</span></div>`).join("")}
        </div>
      </div>
    </section>
    <section class="coluna-info" data-lab="info" data-rolagem="12">
      <div class="bloco" id="d-sobre" data-foco>
        <div class="rotulo">Sobre</div>
        <p>${esc(p.descricao)}</p>
      </div>
      <div class="bloco" id="d-ficha" data-foco>
        <div class="rotulo">Ficha</div>
        <dl>
          <dt>Tipo</dt><dd>${p.tipo === "recompilado" ? "Recompilado: o código do jogo foi traduzido pra rodar nativo." : "Decompilado: o código do jogo foi refeito à mão a partir do original."}</dd>
          <dt>Versão</dt><dd>${esc(p.versao === "—" ? "não instalado" : p.versao)}</dd>
          <dt>Tamanho</dt><dd>${esc(p.tamanho)}</dd>
          <dt>Jogo original</dt><dd>${esc(p.plataforma)}</dd>
        </dl>
      </div>
      <div class="bloco" id="d-origem" data-foco>
        <div class="rotulo">De onde vem</div>
        <p>Feito por <b>${esc(p.repo.split("/")[0])}</b>. O app baixa a versão oficial do projeto no GitHub.</p>
        <p class="fonte">Informações do catálogo de ports. ${p.id === "oot" || p.id === "sm64" ? "Testado pelo catálogo." : "Ainda não testado pelo catálogo."}</p>
        <button type="button" class="btn contorno pequeno" data-foco id="d-github">Ver no GitHub ↗</button>
      </div>
    </section>
    <div class="dialogo-fundo" data-camada data-confirmar data-lab="desinstalar">
      <div class="dialogo" role="alertdialog" aria-labelledby="dlg-t">
        <h3 id="dlg-t">Desinstalar ${esc(p.titulo)}?</h3>
        <p>O port sai do aparelho. O arquivo do jogo e o progresso salvo continuam guardados, então dá pra instalar de novo depois sem perder nada.</p>
        <div class="acoes">
          <button type="button" class="btn contorno" data-foco data-cancelar>${g("b")}Cancelar</button>
          <button type="button" class="btn perigo" data-foco data-desinstalar>Desinstalar</button>
        </div>
      </div>
    </div>`,
});
montarOpcoes(tela);

tela.querySelector("[data-principal]").addEventListener("click", () => (p.estado === "nao-roda" ? irPara("instalar", `?port=${p.id}`) : acaoPrincipal(p)()));
tela.querySelector("[data-mais]").addEventListener("click", () => abrirOpcoes(p, aoOpcao));
tela.querySelector("[data-voltar]").addEventListener("click", () => voltar("ports"));
const confirmar = tela.querySelector("[data-confirmar]");
tela.querySelector("[data-cancelar]").addEventListener("click", () => fecharCamada(confirmar));
tela.querySelector("[data-desinstalar]").addEventListener("click", () => irPara("ports"));

function aoOpcao(id, port) {
  if (id === "desinstalar") return abrirCamada(confirmar, confirmar.querySelector("[data-cancelar]"));
  if (id === "detalhes") return;
  executarOpcao(id, port);
}

tela.addEventListener("focado", (ev) => {
  if (ev.target.closest("[data-camada]")) return trocarDicas([["a", "Escolher"], ["b", "Voltar"]]);
  trocarDicas([["a", ev.target.dataset.principal !== undefined ? acaoTexto : "Escolher"], ["menu", "Opções"], ["b", "Voltar"]]);
});

iniciarFoco({
  inicial: tela.querySelector("[data-principal]"),
  menu: () => !camadaAberta() && abrirOpcoes(p, aoOpcao),
  b: () => {
    const c = camadaAberta();
    if (c === confirmar) return fecharCamada(confirmar);
    if (fecharOpcoes()) return;
    voltar("ports");
  },
});
if (params.get("confirmar") === "desinstalar") abrirCamada(confirmar, confirmar.querySelector("[data-cancelar]"));

montarRefinamento();
