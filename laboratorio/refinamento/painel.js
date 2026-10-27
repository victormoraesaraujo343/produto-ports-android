/* REFINAMENTO — painel de revisão de design do laboratório do portfólio
   (victor-moraes-site/laboratorio/refinamento, copiado em 27/10/2026).
   Mudanças aqui: variação com Shift + ←/→ (as setas são o controle do app).
   Adaptado do laboratório da Bioma (bioma-loja-lab/laboratorio/refinamento).
   Só existe em dev (injetado pelo BaseLayout quando import.meta.env.DEV). Grava em
   `laboratorio/<pagina>/anotacoes.json` + `ANOTACOES.md` pela integração
   `laboratorio/integracao.mjs`; sem ela, fica só no navegador.

   Três layouts do mesmo painel, para o Victor escolher (L alterna):
   A · Coluna  — painel lateral, lista de seções que abre a da tela.
   B · Doca    — barra no pé da página com a seção em vista; o resto em gavetas.
   C · Trilho  — trilho fino de números na borda; a seção abre num cartão.
   Os três desenham as mesmas peças (corpo da seção, mapa, rodadas, rodapé).

   Ciclo: Victor anota → "Fechar rodada" congela todas as versões (com o
   commit) e manda as conversas abertas para o Arquivo → IA responde em
   `versoes[v].secoes[n].resposta`, marcando a seção como "pronta" → Victor
   aprova (uma ou mais variações; a primeira é a principal), pede ajuste ou
   marca a conversa como resolvida. O que fica para trás vai para o Arquivo:
   opções não aprovadas, "R1 · anterior", conversas resolvidas, versões antigas
   e rodadas. Formato no README. */
import "./painel.css";
import { esc, ico, botaoIcone, segmentado, ponto, bloco as blocoHtml, opcao, progresso as progressoHtml, NOME_STATUS } from "../sistema/componentes.js";

const LAYOUTS = [["a", "Coluna"], ["b", "Doca"], ["c", "Trilho"]];
const html = document.documentElement;
const kebab = (k) => k.replace(/[A-Z]/g, (m) => "-" + m.toLowerCase());
const agora = () => new Date().toISOString();
const dataCurta = (iso) => (iso ? new Date(iso).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" }) : "—");
const calmo = () => matchMedia("(prefers-reduced-motion: reduce)").matches;
/* ?lab-demo=a|b|c[&lab-secao=n]: o painel aberto nesse layout, sem gravar nada (miniaturas da
   página do sistema). Fora disso, dentro de um iframe o painel não monta (visor mobile). */
const busca = new URLSearchParams(location.search);
const DEMO = ["a", "b", "c"].includes(busca.get("lab-demo")) ? busca.get("lab-demo") : null;
const dentroDoVisor = window.self !== window.top && !DEMO;
const guarda = { ler(k, pad) { try { return localStorage.getItem(k) ?? pad; } catch { return pad; } }, gravar(k, v) { if (DEMO) return; try { localStorage.setItem(k, v); } catch {} } };
const sessao = { ler(k) { try { const v = sessionStorage.getItem(k); sessionStorage.removeItem(k); return v; } catch { return null; } }, gravar(k, v) { try { sessionStorage.setItem(k, v); } catch {} } };

/* versoes: [{ id, rotulo, rota, disponivel, grupo? }] — rota relativa ao idioma ("" = home).
   Cada URL é uma versão; as antigas da home ficam no grupo "Arquivo" do mapa.
   variantes: { chave: { secao, rotulo, opcoes: [[id, nome, dica?]], versoes?, padrao?, por? } } —
   os controles ficam dentro da seção ("0" = Geral da versão) e ←/→ troca a da seção em vista.
   mapa: [{ grupo, itens: [{ id, rotulo, rota, pagina, versao, secoes }] }] — o índice do
   laboratório: navegar entre páginas, cada uma com o seu estado de revisão. */
export function criarRefinamento({ pagina, secoes: SECOES_BASE = [], versoes: VERSOES, variantes: VARIANTES = {}, mapa: MAPA = [] }) {
  const CHAVE = `refinamento-${pagina}`;
  const API = `/__lab/anotacoes?pagina=${encodeURIComponent(pagina)}`;

  /* ------------------------------------------------------------ versão atual (pela URL) */
  const prefixo = location.pathname.startsWith("/pt/") || location.pathname === "/pt" ? "/pt/" : "/";
  const resto = location.pathname.slice(prefixo.length).replace(/\/$/, "");
  const VA = (VERSOES.find((v) => v.rota === resto) ?? VERSOES[0]).id;
  const rotuloDe = (id) => VERSOES.find((v) => v.id === id)?.rotulo ?? id;
  const secoesDe = (id) => VERSOES.find((v) => v.id === id)?.secoes ?? SECOES_BASE;
  const SECOES = secoesDe(VA);
  const UMA = VERSOES.length === 1;
  const nomeDe = (n) => SECOES.find((s) => s.n === n)?.nome ?? "";

  /* ------------------------------------------------------------ estado */
  const vazio = () => ({ rodada: { numero: 1, aberta_em: agora() }, geral: "", versoes: {}, alfinetes: [], rodadas: [], arquivo: { conversas: [] } });
  const normalizar = (d) => { const e = { ...vazio(), ...d }; for (const v of VERSOES) e.versoes[v.id] ??= { secoes: {} }; return e; };
  let estado = vazio();
  try { estado = normalizar(JSON.parse(guarda.ler(CHAVE, "{}"))); } catch { estado = normalizar({}); }
  const ui = {
    layout: guarda.ler("refinamento-layout", "a"),
    lado: guarda.ler(`${CHAVE}-lado`, "direita"),
    mini: guarda.ler(`${CHAVE}-mini`, "0") === "1",
    numerar: guarda.ler(`${CHAVE}-numerar`, "0") === "1",
    seguir: guarda.ler("refinamento-seguir", "1") === "1",
    foco: null,      // seção aberta (A), na doca (B) ou no cartão (C)
    emVista: null,   // seção que está na tela
    vista: "secoes", // A: "secoes" | "arquivo"
    gaveta: null,    // B: "nota" | "lista" | "mapa" | "arquivo" | "ajuda"
    cartao: null,    // C: "sec" | "mapa" | "arquivo" | "ajuda"
    conversas: new Set(), // conversas abertas (expandidas)
    mapa: false, ajuda: false, confirmar: false, alfinetar: false,
  };
  if (!LAYOUTS.some(([id]) => id === ui.layout)) ui.layout = "a";
  const daVersao = (id = VA) => (estado.versoes[id] ??= { secoes: {} });
  const ler = (n) => daVersao().secoes[n] ?? {};
  /* a seção "0" é o Geral da versão; o Geral de todas as versões fica em `estado.geral` */
  const secao = (n, id = VA) => (daVersao(id).secoes[n] ??= { nome: n === "0" ? "Geral da versão" : nomeDe(n), status: "revisar", nota: "", variantes: {} });
  /* uma variante pode valer só para algumas versões (`versoes`) e ter padrão por versão (`padrao`) */
  const vale = (v) => !v.versoes || v.versoes.includes(VA);
  /* `por`: opções diferentes por versão (ex.: navbars da síntese única vs. das anteriores) */
  const opcoesDe = (v) => (v.por?.[VA] ? v.opcoes.filter(([id]) => v.por[VA].includes(id)) : v.opcoes);
  const daSecao = (n) => Object.entries(VARIANTES).filter(([, v]) => v.secao === n && vale(v));
  const ouvintes = new Map();

  function variante(chave) {
    const v = VARIANTES[chave];
    const salvo = daVersao().secoes[v.secao]?.variantes?.[chave];
    const ops = opcoesDe(v);
    return ops.some(([id]) => id === salvo) ? salvo : v.padrao?.[VA] ?? ops[0][0];
  }
  /* nome curto da opção: "A · Anos" → "A"; "Carvão" fica "Carvão" */
  const curto = (chave, id = variante(chave)) => { const nome = opcoesDe(VARIANTES[chave]).find(([x]) => x === id)?.[1] ?? id; return nome.split(" · ")[0]; };
  const aoMudar = (chave, fn) => ouvintes.set(chave, [...(ouvintes.get(chave) ?? []), fn]);
  function aplicar(chave, avisar = true) {
    if (!vale(VARIANTES[chave])) return;
    const valor = variante(chave);
    if (html.getAttribute(`data-${kebab(chave)}`) === valor) return;
    html.setAttribute(`data-${kebab(chave)}`, valor);
    if (avisar) (ouvintes.get(chave) ?? []).forEach((fn) => fn(valor));
  }
  Object.keys(VARIANTES).forEach((k) => aplicar(k, false));
  function escolherVariante(k, valor) {
    const n = VARIANTES[k].secao, s = secao(n);
    /* aprovada antes da lista de aprovadas: fixa a lista antes de trocar o que está na tela */
    if (s.status === "aprovada" && !s.aprovadas) s.aprovadas = Object.fromEntries(daSecao(n).map(([x]) => [x, [variante(x)]]));
    s.variantes[k] = valor;
    aplicar(k); salvar(); desenhar();
    visor?.querySelector("iframe")?.contentWindow?.postMessage({ rf: "variante", chave: k, valor }, location.origin);
  }

  /* ------------------------------------------------------------ gravação */
  let relogio, pendente = false, ultimoAtualizado = estado.atualizado, salvoTexto = "carregando…";
  const corpo = () => JSON.stringify({ ...estado, meta: { pagina, secoes: SECOES, versoes: VERSOES } });
  async function enviar({ manter = false } = {}) {
    try {
      const r = await fetch(API, { method: "POST", headers: { "Content-Type": "application/json" }, body: corpo(), keepalive: manter });
      if (!r.ok) throw new Error(r.status);
      const d = await r.json(); ultimoAtualizado = estado.atualizado = d.atualizado;
      marcar("salvo");
    } catch { marcar("salvo só neste navegador"); }
    pendente = false;
  }
  function salvar() {
    if (DEMO) return;
    guarda.gravar(CHAVE, JSON.stringify(estado));
    clearTimeout(relogio); pendente = true; marcar("salvando…");
    relogio = setTimeout(enviar, 500);
  }
  /* antes de trocar de página: grava já o que estiver pendente */
  async function salvarJa() { if (!pendente) return; clearTimeout(relogio); await enviar({ manter: true }); }
  async function carregar({ silencioso = false } = {}) {
    try {
      const r = await fetch(API, { cache: "no-store" });
      if (!r.ok) throw new Error(r.status);
      const d = await r.json();
      if (d?.versoes && d.atualizado !== ultimoAtualizado && !pendente && !digitando()) {
        const respostas = (e) => Object.entries(e.versoes ?? {}).flatMap(([v, x]) => Object.entries(x.secoes ?? {}).map(([n, s]) => `${v}:${n}:${s.resposta?.em ?? ""}`)).filter((k) => !k.endsWith(":"));
        const antes = new Set(respostas(estado));
        const novasRespostas = respostas(d).filter((k) => !antes.has(k)).length;
        /* o arquivo é a fonte: não misturar com restos do navegador */
        delete d.meta;
        estado = normalizar(d);
        ultimoAtualizado = d.atualizado;
        guarda.gravar(CHAVE, JSON.stringify(estado));
        Object.keys(VARIANTES).forEach((k) => aplicar(k));
        desenhar();
        if (silencioso && novasRespostas) avisar(`${novasRespostas} resposta${novasRespostas > 1 ? "s" : ""} nova${novasRespostas > 1 ? "s" : ""} da IA`);
      }
      if (!silencioso) marcar("ligado");
    } catch { if (!silencioso) marcar("sem servidor: só neste navegador"); }
  }
  const digitando = () => document.activeElement?.matches?.("textarea, input");

  /* ------------------------------------------------------------ elementos */
  let painel, botao, visor, pop;
  const $ = (s, r = painel) => r?.querySelector(s);
  function marcar(t) { salvoTexto = t; painel?.querySelectorAll("[data-salvo]").forEach((el) => (el.textContent = t)); }
  function avisar(t) {
    const el = document.createElement("div"); el.className = "rf-aviso"; el.textContent = t;
    document.body.append(el); requestAnimationFrame(() => el.classList.add("on"));
    setTimeout(() => { el.classList.remove("on"); setTimeout(() => el.remove(), 400); }, 2600);
  }

  const contar = (id = VA) => {
    const vals = secoesDe(id).map((s) => estado.versoes[id]?.secoes?.[s.n]?.status ?? "revisar");
    return { aprovadas: vals.filter((v) => v === "aprovada").length, prontas: vals.filter((v) => v === "pronta").length, ajustar: vals.filter((v) => v === "ajustar").length, total: vals.length };
  };
  const alfinetesDaVersao = () => estado.alfinetes.filter((a) => a.versao === VA);
  const alfinetesDe = (n) => alfinetesDaVersao().filter((a) => a.secao === n);

  /* ------------------------------------------------------------ linhas: Geral + seções */
  /* "G" = Geral de todas as versões (nas páginas, o Geral da página); "0" = Geral da versão (home).
     Na home, o "todas as versões" só aparece se tiver conteúdo. */
  function linhas() {
    const temTodas = estado.geral?.trim() || estado.pedidoGeral?.texto || estado.respostaGeral?.texto;
    const gerais = UMA ? [{ id: "G", rot: "G", nome: "Geral" }]
      : [{ id: "0", rot: "G", nome: "Geral" }, ...(temTodas ? [{ id: "G", rot: "T", nome: "Geral · todas as versões" }] : [])];
    return [...gerais, ...SECOES.map((s) => ({ id: s.n, rot: s.n, nome: s.nome, secao: true }))];
  }
  const linhaDe = (id) => linhas().find((l) => l.id === id);
  function dadosDe(id) {
    if (id === "G") return { nota: estado.geral, vars: [], pins: [] };
    const e = ler(id);
    return { nota: e.nota, status: id === "0" ? null : e.status ?? "revisar", vars: daSecao(id), pins: alfinetesDe(id) };
  }

  /* ------------------------------------------------------------ aprovação */
  /* Uma seção aprovada pode ter mais de uma variação aprovada por chave (`aprovadas[chave]`);
     a primeira é a principal (a que vai para o site). Seções aprovadas antes disso valem
     pela variação que estava escolhida. */
  const ehAprovada = (n) => ler(n).status === "aprovada";
  const aprovadasDe = (n, k) => { const s = ler(n); return s.aprovadas?.[k] ?? (s.status === "aprovada" ? [s.variantes?.[k] ?? variante(k)] : []); };
  const nomeOpcao = (k, id) => opcoesDe(VARIANTES[k]).find(([x]) => x === id)?.[1] ?? id;
  /* opções que aparecem na seção; o resto fica no Arquivo. "r1" (a rodada anterior) é sempre do Arquivo. */
  function opcoesVivas(n, k) {
    const ops = opcoesDe(VARIANTES[k]);
    if (ehAprovada(n)) { const ap = aprovadasDe(n, k); return ap.map((id) => ops.find(([x]) => x === id)).filter(Boolean); }
    return ops.filter(([id]) => id !== "r1" || id === variante(k));
  }
  const opcoesArquivadas = (n, k) => { const vivas = new Set(opcoesVivas(n, k).map(([id]) => id)); return opcoesDe(VARIANTES[k]).filter(([id]) => !vivas.has(id)); };
  /* aprova o que está na tela: soma a variação escolhida de cada chave às aprovadas */
  function aprovar(n) {
    const s = secao(n);
    const vars = daSecao(n);
    if (vars.length) {
      const ap = Object.fromEntries(vars.map(([k]) => { const l = [...aprovadasDe(n, k)]; if (!l.includes(variante(k))) l.push(variante(k)); return [k, l]; }));
      s.aprovadas = { ...(s.aprovadas ?? {}), ...ap };
    }
    s.status = "aprovada";
    arquivarConversa(VA, n, "aprovada");
  }
  function tirarAprovada(n, k, id) {
    const s = secao(n), l = aprovadasDe(n, k).filter((x) => x !== id);
    s.aprovadas = { ...(s.aprovadas ?? {}), [k]: l };
    if (!l.length) { s.status = "revisar"; delete s.aprovadas; }
  }
  function tornarPrincipal(n, k, id) {
    const s = secao(n);
    s.aprovadas = { ...(s.aprovadas ?? {}), [k]: [id, ...aprovadasDe(n, k).filter((x) => x !== id)] };
  }
  function reabrir(n) { const s = secao(n); s.status = "revisar"; delete s.aprovadas; }

  /* ------------------------------------------------------------ conversas: pedido + resposta da IA */
  /* Uma conversa aberta por seção. "Resolvido", aprovar a seção ou fechar a rodada
     manda a conversa para `estado.arquivo.conversas`. */
  function conversaDe(id, versao = VA) {
    const pedido = id === "G" ? estado.pedidoGeral && { rodada: estado.pedidoGeral.rodada, texto: estado.pedidoGeral.texto } : (() => { const p = estado.versoes[versao]?.secoes?.[id]?.pedido; return p && { rodada: p.rodada, texto: p.nota ?? p.texto }; })();
    const resposta = id === "G" ? estado.respostaGeral : estado.versoes[versao]?.secoes?.[id]?.resposta;
    return pedido?.texto || resposta?.texto ? { pedido: pedido?.texto ? pedido : null, resposta: resposta?.texto ? resposta : null } : null;
  }
  function arquivarConversa(versao, id, motivo) {
    const c = conversaDe(id, versao); if (!c) return;
    const arq = (estado.arquivo ??= {}); (arq.conversas ??= []);
    const nome = id === "G" ? (UMA ? "Geral" : "Geral · todas as versões") : id === "0" ? "Geral da versão" : secoesDe(versao).find((s) => s.n === id)?.nome ?? id;
    arq.conversas.push({ versao: id === "G" ? null : versao, secao: id, nome, ...c, motivo, em: agora() });
    if (id === "G") { estado.pedidoGeral = null; delete estado.respostaGeral; }
    else { const s = estado.versoes[versao].secoes[id]; delete s.pedido; delete s.resposta; }
  }
  function reabrirConversa(i) {
    const c = estado.arquivo?.conversas?.[i]; if (!c) return;
    if (conversaDe(c.secao, c.versao ?? VA)) return avisar("Essa seção já tem uma conversa aberta");
    if (c.secao === "G") { estado.pedidoGeral = c.pedido && { rodada: c.pedido.rodada, texto: c.pedido.texto }; if (c.resposta) estado.respostaGeral = c.resposta; }
    else { const s = secao(c.secao, c.versao); if (c.pedido) s.pedido = { rodada: c.pedido.rodada, nota: c.pedido.texto }; if (c.resposta) s.resposta = c.resposta; }
    estado.arquivo.conversas.splice(i, 1);
  }

  /* ------------------------------------------------------------ peças comuns */
  const bloco = blocoHtml;
  const pontoDe = (id) => ponto(dadosDe(id).status);
  /* o que a seção mostra fechada: as aprovadas (★ = principal) ou a escolhida */
  function escolhasDe(id) {
    const vars = dadosDe(id).vars;
    if (!vars.length) return "";
    return vars.map(([k]) => (ehAprovada(id) ? aprovadasDe(id, k).map((x) => curto(k, x)).join(", ") : curto(k))).join(" · ");
  }
  function sinais(id) {
    const d = dadosDe(id), c = conversaDe(id);
    const abertos = d.pins.filter((a) => !a.resolvido).length;
    return `${c?.resposta ? `<span class="rf-sinal ia" title="A IA respondeu">${ico("ia")}</span>` : ""}${abertos ? `<span class="rf-sinal" title="${abertos} alfinete(s) aberto(s)">${ico("pin")}${abertos}</span>` : ""}${d.nota?.trim() ? `<span class="rf-sinal" title="Tem anotação">${ico("nota")}</span>` : ""}${d.vars.length ? `<span class="rf-sinal rf-sinal-var">${ehAprovada(id) ? ico("ok") : ""}${esc(escolhasDe(id))}</span>` : ""}`;
  }
  const botaoOpcao = (k, id, nome, dica, { compacto = false } = {}) => opcao({ chave: k, id, nome, dica, atual: variante(k) === id, compacto });
  /* variações da seção em aberto: só as vivas; as do Arquivo ficam lá */
  function controles(id, { compacto = false } = {}) {
    return dadosDe(id).vars.map(([k, v]) => {
      const ops = opcoesVivas(id, k);
      const dica = ops.find(([x]) => x === variante(k))?.[2];
      return `<div class="rf-var${compacto ? " compacto" : ""}"><span>${esc(v.rotulo)}</span><div class="rf-opcoes" style="--n:${ops.length}">${ops.map(([x, nome, d]) => botaoOpcao(k, x, nome, d, { compacto })).join("")}</div>${dica && !compacto ? `<p class="rf-dica">${esc(dica)}</p>` : ""}</div>`;
    }).join("");
  }
  /* seção aprovada: as aprovadas (★ principal, × tira), "aprovar também" se estiver vendo outra,
     e as outras opções recolhidas */
  function aprovadasHtml(id, { compacto = false } = {}) {
    return dadosDe(id).vars.map(([k, v]) => {
      const ap = aprovadasDe(id, k), atual = variante(k);
      const chips = ap.map((x, i) => `<span class="rf-chip${atual === x ? " atual" : ""}">
          <button type="button" data-var="${k}" data-valor="${x}" title="Ver na página">${i === 0 ? '<i aria-label="principal">★</i>' : ""}${esc(compacto ? curto(k, x) : nomeOpcao(k, x))}</button>
          ${compacto ? "" : `${i ? `<button type="button" class="rf-chip-ac" data-acao="principal" data-arg="${id}|${k}|${x}" title="Tornar principal" aria-label="Tornar principal">★</button>` : ""}<button type="button" class="rf-chip-ac" data-acao="tirar" data-arg="${id}|${k}|${x}" title="Tirar da aprovação" aria-label="Tirar da aprovação">${ico("fechar")}</button>`}
        </span>`).join("");
      const outras = opcoesArquivadas(id, k);
      const vendoOutra = !ap.includes(atual);
      return `<div class="rf-var${compacto ? " compacto" : ""}"><span>${esc(v.rotulo)}</span><div class="rf-chips">${chips}</div>
        ${!compacto && vendoOutra ? `<p class="rf-vendo">Na página: <b>${esc(nomeOpcao(k, atual))}</b> <button type="button" class="rf-link" data-acao="aprovar" data-arg="${id}">aprovar também</button></p>` : ""}
        ${!compacto && outras.length ? `<div class="rf-outras"><span>Outras, no arquivo:</span>${outras.map(([x, nome, d]) => botaoOpcao(k, x, nome, d, { compacto: true })).join("")}</div>` : ""}</div>`;
    }).join("");
  }
  /* Aprovar e Ajustar; numa aprovada, só "Reabrir" */
  function decisao(id, { compacto = false } = {}) {
    const d = dadosDe(id); if (!d.status) return "";
    if (d.status === "aprovada") {
      const vendoOutra = d.vars.some(([k]) => !aprovadasDe(id, k).includes(variante(k)));
      return `<div class="rf-decisao"><span class="rf-aprovada-selo">${ico("ok")}Aprovada</span>${compacto && vendoOutra ? `<button type="button" class="rf-aprovar" data-acao="aprovar" data-arg="${id}">Aprovar também</button>` : ""}${compacto ? "" : `<button type="button" class="rf-link" data-acao="reabrir" data-arg="${id}">reabrir</button>`}</div>`;
    }
    const esc_ = d.vars.map(([k]) => curto(k)).join(" ");
    return `<div class="rf-decisao">
      <button type="button" class="rf-aprovar" data-acao="aprovar" data-arg="${id}">${ico("ok")}Aprovar${esc_ ? ` <b>${esc(esc_)}</b>` : ""}</button>
      <button type="button" class="rf-ajustar" data-acao="ajustar" data-arg="${id}" aria-pressed="${d.status === "ajustar"}">Ajustar</button>
      ${d.status === "pronta" ? `<span class="rf-selo">IA respondeu</span>` : ""}
    </div>`;
  }
  /* conversa recolhida numa linha; abre o texto inteiro */
  function conversa(id) {
    const c = conversaDe(id); if (!c) return "";
    const aberta = ui.conversas.has(id);
    const r = c.pedido?.rodada ?? c.resposta?.rodada;
    const resumo = c.resposta ? `IA: ${c.resposta.texto}` : `Você: ${c.pedido.texto}`;
    return `<div class="rf-conversa${aberta ? " aberta" : ""}">
      <button type="button" class="rf-conversa-cab" data-acao="conversa" data-arg="${id}" aria-expanded="${aberta}"><span>${r ? `R${r} · ` : ""}${c.pedido ? "você pediu" : ""}${c.pedido && c.resposta ? " → " : ""}${c.resposta ? "IA respondeu" : ""}</span>${aberta ? "" : `<em>${esc(resumo)}</em>`}</button>
      ${aberta ? `${c.pedido ? bloco("pedido inteiro", `Seu pedido${c.pedido.rodada ? ` · R${c.pedido.rodada}` : ""}`, c.pedido.texto) : ""}${c.resposta ? bloco("resposta inteiro", `IA · ${dataCurta(c.resposta.em)}`, c.resposta.texto) : ""}` : ""}
      <button type="button" class="rf-link rf-resolver" data-acao="resolver" data-arg="${id}">${ico("ok")}resolvido</button>
    </div>`;
  }
  const campoNota = (id, rows = 2) => `<textarea rows="${rows}" data-nota-de="${id}" aria-label="Anotação · ${esc(linhaDe(id)?.nome ?? id)}" placeholder="Anotação">${esc(dadosDe(id).nota ?? "")}</textarea>`;
  function pinos(id) {
    const ps = dadosDe(id).pins; if (!ps.length) return "";
    return `<ul class="rf-pins">${ps.map((a) => `<li class="${a.resolvido ? "feito" : ""}"><button type="button" data-ver-pin="${a.id}">${ico("pin")} ${vistaDoPin(a) === "mobile" ? "<b>mobile</b> · " : ""}${esc(a.alvo || "ponto")}</button><em>${esc(a.texto || "sem texto")}</em></li>`).join("")}</ul>`;
  }
  /* corpo completo de uma linha (A, C e a gaveta da B) */
  const corpoDe = (id) => `<div class="rf-corpo">${conversa(id)}${ehAprovada(id) ? `${decisao(id)}${aprovadasHtml(id)}` : `${controles(id)}${decisao(id)}`}${campoNota(id)}${pinos(id)}</div>`;

  const ferramenta = (acao, icone, rotulo, atalho, ligado) => botaoIcone({ acao, icone, rotulo, atalho, ligado });
  const ferramentas = () => ferramenta("alfinete", "pin", "Alfinete", "P", ui.alfinetar) + ferramenta("mobile", "mobile", "Mobile", "V", V.aberto) + ferramenta("numerar", "numero", "Numerar seções", "N", ui.numerar) + ferramenta("seguir", "seguir", "Acompanhar a rolagem", "S", ui.seguir);
  const botaoIndice = () => botaoIcone({ acao: "indice", icone: "indice", rotulo: "Índice de páginas", atalho: "I" }) + botaoIcone({ acao: "sistema", icone: "sistema", rotulo: "Sistema do laboratório", atalho: "D" });
  const trocaLayout = () => segmentado({ opcoes: LAYOUTS.map(([id, nome]) => [id, `${id.toUpperCase()} · ${nome}  L`]), atual: ui.layout, acao: "layout", rotulo: "Layout do painel" });
  function progresso() {
    const pedido = estado.pedidoDeRevisao && !estado.pedidoDeRevisao.atendido;
    return progressoHtml({ ...contar(), extra: pedido ? ` · R${estado.pedidoDeRevisao.rodada} com a IA` : "" });
  }
  const fecharRodadaBotao = () => ui.confirmar
    ? `<div class="rf-confirma"><span>Fechar a R${estado.rodada.numero}${UMA ? "" : " de todas as versões"}? As conversas abertas vão para o Arquivo.</span><button type="button" class="rf-primario" data-acao="rodada-sim">Fechar</button><button type="button" class="rf-link" data-acao="rodada-nao">Cancelar</button></div>`
    : `<button type="button" class="rf-secundario" data-acao="rodada">Fechar R${estado.rodada.numero}</button>`;
  const atalhos = () => `<div class="rf-atalhos"><span><kbd>Shift</kbd><kbd>←</kbd><kbd>→</kbd> variação da seção na tela</span><span><kbd>R</kbd> abre/fecha</span><span><kbd>I</kbd> índice</span><span><kbd>D</kbd> sistema</span><span><kbd>P</kbd> alfinete</span><span><kbd>V</kbd> mobile</span><span><kbd>N</kbd> numera</span><span><kbd>S</kbd> acompanha a rolagem</span><span><kbd>L</kbd> troca o layout</span>${ui.layout === "a" ? `<span><kbd>M</kbd> minimiza</span><span><kbd>[</kbd><kbd>]</kbd> lado</span>` : ""}<span><kbd>Esc</kbd> sai</span></div>`;

  /* ------------------------------------------------------------ Arquivo: o que ficou para trás */
  function arquivoHtml({ comRodada = false } = {}) {
    const vars = SECOES.flatMap((s) => daSecao(s.n).map(([k, v]) => ({ n: s.n, nome: s.nome, k, v, ops: opcoesArquivadas(s.n, k) }))).filter((x) => x.ops.length);
    const conversas = (estado.arquivo?.conversas ?? []).map((c, i) => ({ ...c, i })).filter((c) => !c.versao || c.versao === VA).reverse();
    const antigas = MAPA.find((g) => g.grupo === "Arquivo")?.itens ?? [];
    const titulo = (t, n) => `<p class="rf-arq-t">${t}${n != null ? ` <small>${n}</small>` : ""}</p>`;
    return `${comRodada ? `<div class="rf-arq-rodada">${progresso()}<div class="rf-rodape">${fecharRodadaBotao()}${ui.confirmar ? "" : `<small class="rf-salvo" data-salvo>${esc(salvoTexto)}</small>`}</div></div>` : ""}
      ${titulo("Variações", vars.reduce((t, x) => t + x.ops.length, 0))}
      ${vars.length ? vars.map((x) => `<div class="rf-arq-var"><span class="rf-num">${x.n}</span><div><b>${esc(x.nome)}</b>${daSecao(x.n).length > 1 ? ` <small>${esc(x.v.rotulo)}</small>` : ""}<div class="rf-arq-ops">${x.ops.map(([id, nome, d]) => `<button type="button" data-acao="ver" data-arg="${x.n}|${x.k}|${id}" aria-pressed="${variante(x.k) === id}" title="${esc(d ?? "Ver na página")}">${esc(nome)}</button>`).join("")}</div></div></div>`).join("") : `<p class="rf-vazio">Nada por aqui.</p>`}
      ${titulo("Conversas resolvidas", conversas.length)}
      ${conversas.length ? conversas.map((c) => `<div class="rf-arq-conv"><div class="rf-arq-conv-cab"><span class="rf-num">${c.secao === "G" ? "G" : c.secao === "0" ? "G" : c.secao}</span><b>${esc(c.nome)}</b><small>${c.pedido?.rodada ? `R${c.pedido.rodada}` : ""}</small><button type="button" class="rf-link" data-acao="reabrir-conversa" data-arg="${c.i}">reabrir</button></div>${c.pedido ? bloco("pedido", "Seu pedido", c.pedido.texto) : ""}${c.resposta ? bloco("resposta", "IA", c.resposta.texto) : ""}</div>`).join("") : `<p class="rf-vazio">Nenhuma.</p>`}
      ${antigas.length ? `${titulo("Versões antigas", antigas.length)}<ul class="rf-arq-links">${antigas.map((it) => `<li><a href="${hrefDe(it.rota)}" data-mapa-ir${estaAqui(it) ? ' aria-current="page"' : ""}>${esc(it.rotulo)}</a></li>`).join("")}</ul>` : ""}
      ${titulo("Rodadas fechadas", estado.rodadas.length)}
      <div class="rf-lista-rod">${listaDeRodadas()}</div>`;
  }

  /* ------------------------------------------------------------ rodadas */
  function listaDeRodadas() {
    if (!estado.rodadas.length) return `<p class="rf-vazio">Nenhuma rodada fechada.</p>`;
    return [...estado.rodadas].reverse().map((r) => {
      const porVersao = Object.entries(r.versoes ?? {}).map(([v, x]) => {
        const itens = Object.entries(x.secoes ?? {}).filter(([, s]) => s.nota?.trim() || (s.status && s.status !== "revisar"));
        const pins = (r.alfinetes ?? []).filter((a) => a.versao === v && a.texto?.trim()).length;
        if (!itens.length && !pins) return "";
        return `<div class="rf-rod-versao"><b>${esc(rotuloDe(v))}</b>${pins ? `<small>${pins} alfinete${pins > 1 ? "s" : ""}</small>` : ""}</div>` +
          itens.map(([n, s]) => `<div class="rf-rod-item"><span class="rf-num">${n === "0" ? "G" : n}</span><div><b>${esc(n === "0" ? "Geral da versão" : s.nome ?? nomeDe(n))}</b> <i data-status="${s.status}">${NOME_STATUS[s.status] ?? s.status}</i>${s.nota?.trim() ? `<p>${esc(s.nota)}</p>` : ""}</div></div>`).join("");
      }).join("");
      return `<details class="rf-rod">
        <summary><b>R${r.numero}</b><span>${dataCurta(r.fechada_em)}</span><code>${esc(r.commit?.curto ?? "sem git")}${r.commit?.sujo ? "+" : ""}</code></summary>
        <div class="rf-corpo">
          ${r.geral?.trim() ? bloco("pedido", "Geral · todas", r.geral) : ""}
          ${porVersao || (r.geral?.trim() ? "" : `<p class="rf-vazio">Sem anotações.</p>`)}
        </div>
      </details>`;
    }).join("");
  }

  /* ------------------------------------------------------------ mapa: o índice do laboratório */
  const itensMapa = MAPA.flatMap((g) => g.itens);
  const estaAqui = (it) => it.pagina === pagina && (it.versao === VA || UMA);
  const aqui = () => itensMapa.find(estaAqui);
  const hrefDe = (rota) => (rota.endsWith(".html") ? `/${rota}` : prefixo + (rota ? `${rota}/` : ""));
  let resumo = {};
  function estadoDe(it) {
    const r = it.pagina === pagina ? { secoes: Object.fromEntries(Object.entries(daVersao(it.versao === "pagina" ? VA : it.versao).secoes).map(([n, s]) => [n, s.status])), pins: estado.alfinetes.filter((a) => (UMA || a.versao === it.versao) && !a.resolvido).length } : resumo[it.pagina]?.[it.versao] ?? { secoes: {}, pins: 0 };
    const st = it.secoes.map((n) => r.secoes?.[n] ?? "revisar");
    const aprovadas = st.filter((x) => x === "aprovada").length, ajustar = st.filter((x) => x === "ajustar").length;
    const tipo = aprovadas === st.length ? "aprovada" : ajustar || r.pins ? "ajustar" : aprovadas ? "pronta" : "revisar";
    const texto = tipo === "aprovada" ? "aprovada" : [ajustar && `${ajustar} ajustar`, r.pins && `${r.pins} pin${r.pins > 1 ? "s" : ""}`, `${aprovadas}/${st.length}`].filter(Boolean).join(" · ");
    return { tipo, texto };
  }
  const nomeDaPagina = () => aqui()?.rotulo ?? rotuloDe(VA);
  function mapaLista() {
    if (!MAPA.length) return `<p class="rf-vazio">Sem outras páginas.</p>`;
    return `<div class="rf-mapa-lista">${MAPA.map((g) => `<p class="rf-mapa-g">${esc(g.grupo)}</p><ul>${g.itens.map((it) => { const e = estadoDe(it); return `<li><a href="${hrefDe(it.rota)}" data-mapa-ir${estaAqui(it) ? ' aria-current="page"' : ""}><span class="rf-ponto" data-status="${e.tipo}"></span><b>${esc(it.rotulo)}</b><small>${esc(e.texto)}</small></a></li>`; }).join("")}</ul>`).join("")}
      <a class="rf-mapa-todas" href="${prefixo}indice/">Índice completo</a></div>`;
  }
  async function carregarResumo() {
    const outras = [...new Set(itensMapa.map((i) => i.pagina).filter((p) => p !== pagina))];
    if (!outras.length) return;
    try {
      const r = await fetch(`/__lab/resumo?paginas=${outras.join(",")}`, { cache: "no-store" });
      if (!r.ok) return;
      resumo = await r.json();
      if (painel && !painel.hidden) desenhar();
    } catch {}
  }

  /* ------------------------------------------------------------ layout A · Coluna */
  function linhaA(l) {
    const aberto = ui.foco === l.id;
    return `<div class="rf-linha${aberto ? " aberta" : ""}${ui.emVista === l.id ? " em-vista" : ""}${ehAprovada(l.id) ? " aprovada" : ""}" data-linha="${l.id}">
      <button type="button" class="rf-linha-cab" data-acao="linha" data-arg="${l.id}" aria-expanded="${aberto}"><span class="rf-num">${l.rot}</span><span class="rf-nome">${esc(l.nome)}</span><span class="rf-sinais">${sinais(l.id)}</span>${pontoDe(l.id)}</button>
      ${aberto ? corpoDe(l.id) : ""}
    </div>`;
  }
  function desenharA() {
    return `<header class="rf-cab">
        <button type="button" class="rf-pag" data-acao="mapa" aria-expanded="${ui.mapa}" title="Páginas"><b>${esc(nomeDaPagina())}</b><em>R${estado.rodada.numero}</em><span class="rf-seta" aria-hidden="true"></span></button>
        <div class="rf-acoes">
          ${botaoIndice()}
          <button type="button" class="rf-x" data-acao="lado" title="Mudar de lado  [ ]" aria-label="Mudar o painel de lado">${ico("lado")}</button>
          <button type="button" class="rf-x" data-acao="mini" title="Minimizar  M" aria-label="Minimizar o painel">${ico(ui.mini ? "cima" : "mini")}</button>
          <button type="button" class="rf-x" data-acao="fechar" title="Fechar  R" aria-label="Fechar o laboratório">${ico("fechar")}</button>
        </div>
      </header>
      ${ui.mapa ? `<div class="rf-mapa-pop" data-rolar="mapa">${mapaLista()}</div>` : ""}
      <div class="rf-resto">
        <div class="rf-barra-ferr">${ferramentas()}<span class="rf-flex"></span>${trocaLayout()}</div>
        ${ui.vista === "arquivo"
          ? `<div class="rf-sub"><button type="button" class="rf-link" data-acao="arquivo">${ico("esq")} Seções</button><b>Arquivo</b></div><div class="rf-lista rf-rolagem rf-arq" data-rolar="arquivo">${arquivoHtml()}</div>`
          : `<div class="rf-lista rf-rolagem" data-rolar="lista">${linhas().map(linhaA).join("")}</div>`}
        <footer>
          ${progresso()}
          <div class="rf-rodape">${fecharRodadaBotao()}${ui.confirmar ? "" : `<button type="button" class="rf-link" data-acao="arquivo">${ico("arquivo")}Arquivo</button><small class="rf-salvo" data-salvo>${esc(salvoTexto)}</small><button type="button" class="rf-x rf-ajuda" data-acao="ajuda" aria-expanded="${ui.ajuda}" title="Atalhos" aria-label="Atalhos de teclado">${ico("ajuda")}</button>`}</div>
          ${ui.ajuda ? atalhos() : ""}
        </footer>
      </div>`;
  }

  /* ------------------------------------------------------------ layout B · Doca */
  const focoB = () => ui.foco ?? ui.emVista ?? SECOES[0]?.n ?? "G";
  function gavetaB() {
    const id = focoB(), l = linhaDe(id);
    const titulo = { nota: `${l?.rot ?? ""} · ${esc(l?.nome ?? "")}`, lista: "Seções", mapa: "Páginas", arquivo: "Arquivo", ajuda: "Atalhos" }[ui.gaveta];
    let miolo = "";
    if (ui.gaveta === "nota") miolo = corpoDe(id);
    if (ui.gaveta === "lista") miolo = `<div class="rf-lista-b">${linhas().map((x) => `<button type="button" class="rf-item${x.id === id ? " atual" : ""}${ui.emVista === x.id ? " em-vista" : ""}${ehAprovada(x.id) ? " aprovada" : ""}" data-acao="ir" data-arg="${x.id}"><span class="rf-num">${x.rot}</span><span class="rf-nome">${esc(x.nome)}</span><span class="rf-sinais">${sinais(x.id)}</span>${pontoDe(x.id)}</button>`).join("")}</div>
      <div class="rf-gaveta-pe">${progresso()}<div class="rf-rodape">${fecharRodadaBotao()}${ui.confirmar ? "" : `<button type="button" class="rf-link" data-acao="gaveta" data-arg="arquivo">${ico("arquivo")}Arquivo</button><small class="rf-salvo" data-salvo>${esc(salvoTexto)}</small>`}</div></div>`;
    if (ui.gaveta === "mapa") miolo = mapaLista();
    if (ui.gaveta === "arquivo") miolo = `<div class="rf-arq">${arquivoHtml({ comRodada: true })}</div>`;
    if (ui.gaveta === "ajuda") miolo = atalhos();
    return `<div class="rf-gaveta rf-superficie rf-rolagem" data-rolar="gaveta"><div class="rf-gaveta-cab"><b>${titulo}</b><button type="button" class="rf-x" data-acao="gaveta" data-arg="${ui.gaveta}" aria-label="Fechar">${ico("baixo")}</button></div>${miolo}</div>`;
  }
  function desenharB() {
    const id = focoB(), l = linhaDe(id) ?? { rot: "", nome: "" }, d = dadosDe(id);
    const dica = ehAprovada(id) ? "" : d.vars.map(([k, v]) => opcoesDe(v).find(([x]) => x === variante(k))?.[2]).filter(Boolean).join("  ·  ");
    const temNota = d.nota?.trim() || conversaDe(id) || d.pins.length;
    const g = (arg, rotulo, icone) => `<button type="button" class="rf-x" data-acao="gaveta" data-arg="${arg}" aria-pressed="${ui.gaveta === arg}" title="${rotulo}" aria-label="${rotulo}">${ico(icone)}</button>`;
    return `${ui.gaveta ? gavetaB() : ""}
      <div class="rf-doca rf-superficie">
        <div class="rf-doca-linha">
          <div class="rf-passo">
            <button type="button" class="rf-x" data-acao="passo" data-arg="-1" title="Seção anterior" aria-label="Seção anterior">${ico("esq")}</button>
            <button type="button" class="rf-atual" data-acao="gaveta" data-arg="lista" aria-pressed="${ui.gaveta === "lista"}" title="Todas as seções"><span class="rf-num">${l.rot}</span><span class="rf-nome">${esc(l.nome)}</span>${pontoDe(id)}</button>
            <button type="button" class="rf-x" data-acao="passo" data-arg="1" title="Próxima seção" aria-label="Próxima seção">${ico("dir")}</button>
          </div>
          <div class="rf-doca-vars">${d.vars.length ? (ehAprovada(id) ? aprovadasHtml(id, { compacto: true }) : controles(id, { compacto: true })) : `<span class="rf-sem-vars">Sem variações nesta seção</span>`}</div>
          ${decisao(id, { compacto: true })}
          <button type="button" class="rf-x rf-nota-b${temNota ? " tem" : ""}" data-acao="gaveta" data-arg="nota" aria-pressed="${ui.gaveta === "nota"}" title="Seção inteira: anotação, conversa, outras opções" aria-label="Seção inteira">${ico("nota")}</button>
        </div>
        <div class="rf-doca-meta">
          <button type="button" class="rf-pag-b" data-acao="gaveta" data-arg="mapa" aria-pressed="${ui.gaveta === "mapa"}" title="Páginas"><b>${esc(nomeDaPagina())}</b><em>R${estado.rodada.numero}</em><span class="rf-seta" aria-hidden="true"></span></button>
          <p class="rf-doca-dica">${esc(dica)}</p>
          <small class="rf-salvo" data-salvo>${esc(salvoTexto)}</small>
          <div class="rf-doca-ferr">${ferramentas()}${botaoIndice()}${g("arquivo", "Arquivo", "arquivo")}${g("ajuda", "Atalhos", "ajuda")}${trocaLayout()}<button type="button" class="rf-x" data-acao="fechar" title="Fechar  R" aria-label="Fechar o laboratório">${ico("fechar")}</button></div>
        </div>
      </div>`;
  }

  /* ------------------------------------------------------------ layout C · Trilho */
  function cartaoC() {
    const id = ui.foco ?? ui.emVista ?? SECOES[0]?.n;
    const l = linhaDe(id);
    const titulo = ui.cartao === "sec" ? `<span class="rf-num">${l?.rot ?? ""}</span><b>${esc(l?.nome ?? "")}</b>` : `<b>${{ mapa: "Páginas", arquivo: "Arquivo", ajuda: "Atalhos" }[ui.cartao]}</b>`;
    let miolo = "";
    if (ui.cartao === "sec") miolo = corpoDe(id);
    if (ui.cartao === "mapa") miolo = mapaLista();
    if (ui.cartao === "arquivo") miolo = `<div class="rf-arq">${arquivoHtml({ comRodada: true })}</div>`;
    if (ui.cartao === "ajuda") miolo = `${atalhos()}<div class="rf-cartao-layout"><span>Layout do painel</span>${trocaLayout()}</div>`;
    return `<section class="rf-cartao rf-superficie" data-tipo="${ui.cartao}"><header>${titulo}${ui.cartao === "sec" ? `<span class="rf-sinais">${sinais(id)}</span>` : ""}<button type="button" class="rf-x" data-acao="cartao" data-arg="${ui.cartao}" aria-label="Fechar o cartão">${ico("fechar")}</button></header><div class="rf-cartao-corpo rf-rolagem" data-rolar="cartao">${miolo}</div></section>`;
  }
  function desenharC() {
    const { aprovadas, total } = contar();
    const t = (acao, arg, icone, rotulo, ligado) => `<button type="button" class="rf-x" data-acao="${acao}" data-arg="${arg}" aria-pressed="${!!ligado}" title="${rotulo}" aria-label="${rotulo}">${ico(icone)}</button>`;
    return `<nav class="rf-trilho rf-superficie" aria-label="Seções">
        ${t("cartao", "mapa", "paginas", `Páginas · ${esc(nomeDaPagina())}`, ui.cartao === "mapa")}
        ${botaoIndice()}
        <small class="rf-t-prog" title="${aprovadas} de ${total} aprovadas">${aprovadas}/${total}</small>
        <div class="rf-t-lista" data-rolar="trilho">${linhas().map((l) => { const d = dadosDe(l.id); return `<button type="button" class="rf-t-n${l.id === (ui.cartao === "sec" ? ui.foco : null) ? " atual" : ""}${ui.emVista === l.id ? " em-vista" : ""}${ehAprovada(l.id) ? " aprovada" : ""}" data-acao="trilho" data-arg="${l.id}" title="${esc(l.nome)}${d.status ? ` · ${NOME_STATUS[d.status]}` : ""}"><b>${l.rot}</b>${d.status ? `<i class="rf-ponto" data-status="${d.status}"></i>` : ""}${d.vars.length ? `<small>${esc(escolhasDe(l.id))}</small>` : ""}${conversaDe(l.id) || d.pins.some((a) => !a.resolvido) || d.nota?.trim() ? `<em aria-hidden="true"></em>` : ""}</button>`; }).join("")}</div>
        <div class="rf-t-ferr">${ferramentas()}${t("cartao", "arquivo", "arquivo", `Arquivo · R${estado.rodada.numero}`, ui.cartao === "arquivo")}${t("cartao", "ajuda", "ajuda", "Atalhos e layout", ui.cartao === "ajuda")}<button type="button" class="rf-x" data-acao="fechar" title="Fechar  R" aria-label="Fechar o laboratório">${ico("fechar")}</button></div>
      </nav>
      ${ui.cartao ? cartaoC() : ""}`;
  }
  /* ------------------------------------------------------------ desenho */
  function desenhar() {
    if (!painel) return;
    const rolagens = Object.fromEntries([...painel.querySelectorAll("[data-rolar]")].map((el) => [el.dataset.rolar, el.scrollTop]));
    const focado = document.activeElement && painel.contains(document.activeElement) ? chaveDe(document.activeElement) : null;
    painel.dataset.layout = ui.layout;
    painel.dataset.lado = ui.lado;
    painel.classList.toggle("mini", ui.layout === "a" && ui.mini);
    painel.classList.toggle("rf-superficie", ui.layout === "a");
    painel.classList.toggle("com-cartao", !!ui.cartao);
    painel.innerHTML = ui.layout === "b" ? desenharB() : ui.layout === "c" ? desenharC() : desenharA();
    painel.querySelectorAll("[data-rolar]").forEach((el) => { if (rolagens[el.dataset.rolar]) el.scrollTop = rolagens[el.dataset.rolar]; });
    if (focado) painel.querySelector(focado)?.focus({ preventScroll: true });
    const { prontas } = contar();
    botao.querySelector("b").textContent = prontas ? `${prontas}` : "";
    numerar(); desenharAlfinetes();
  }
  /* devolve o foco do teclado ao mesmo botão depois de redesenhar */
  function chaveDe(el) {
    const d = el.dataset;
    if (d.var) return `[data-var="${d.var}"][data-valor="${d.valor}"]`;
    if (d.acao) return `[data-acao="${d.acao}"]${d.arg != null ? `[data-arg="${d.arg}"]` : ""}`;
    return null;
  }
  /* rola a lista do painel (sem rolar a página) até a linha */
  function mostrarNaLista(sel) {
    const el = painel?.querySelector(sel), box = el?.closest("[data-rolar]");
    if (!el || !box) return;
    const r = el.getBoundingClientRect(), b = box.getBoundingClientRect();
    if (r.top < b.top) box.scrollTop -= b.top - r.top + 8;
    else if (r.bottom > b.bottom) box.scrollTop += Math.min(r.bottom - b.bottom + 8, r.top - b.top - 8);
  }

  /* ------------------------------------------------------------ foco e rolagem */
  let travaAte = 0;
  function irParaSecao(id) {
    travaAte = Date.now() + 900;
    ui.emVista = id;
    if (id === "G") return;
    const el = id === "0" ? null : document.querySelector(`[data-secao="${id}"]`);
    if (id === "0") scrollTo({ top: 0, behavior: calmo() ? "auto" : "smooth" });
    else el?.scrollIntoView({ behavior: calmo() ? "auto" : "smooth", block: "start" });
  }
  function aoRolar() {
    if (!painel || painel.hidden || Date.now() < travaAte) return;
    const { n } = secaoEmVista();
    const id = n ?? (UMA ? null : "0");
    if (!id || id === ui.emVista) return;
    ui.emVista = id;
    const segue = ui.seguir && !digitando() && !pop && !ui.confirmar;
    if (segue && (ui.layout === "b" || (ui.layout === "a" && ui.vista === "secoes" && !ui.mini) || (ui.layout === "c" && ui.cartao === "sec"))) ui.foco = id;
    desenhar();
    if (ui.layout === "a") mostrarNaLista(`[data-linha="${id}"]`);
    if (ui.layout === "c") mostrarNaLista(`.rf-t-n[data-arg="${id}"]`);
  }
  let quadro = 0;
  addEventListener("scroll", () => { cancelAnimationFrame(quadro); quadro = requestAnimationFrame(aoRolar); }, { passive: true });

  /* ------------------------------------------------------------ ←/→: variação da seção em vista */
  function trocarVariacaoEmVista(passo) {
    const n = ui.layout === "b" ? focoB() : secaoEmVista().n;
    const [k, v] = daSecao(n ?? "")[0] ?? [];
    if (!k) return avisar(n ? `Seção ${n} sem variações` : "Nenhuma seção em vista");
    const ops = opcoesDe(v), i = ops.findIndex(([id]) => id === variante(k));
    const [id, nome] = ops[(i + passo + ops.length) % ops.length];
    escolherVariante(k, id);
    avisar(`${n} · ${v.rotulo}: ${nome}`);
  }

  /* ------------------------------------------------------------ numeração das seções */
  function numerar() {
    document.querySelectorAll(".rf-marca").forEach((m) => m.remove());
    html.classList.toggle("rf-numerado", ui.numerar);
    if (!ui.numerar) return;
    document.querySelectorAll("[data-secao]").forEach((el) => {
      if (getComputedStyle(el).position === "static") el.style.position = "relative";
      const m = document.createElement("button");
      m.type = "button"; m.className = "rf-marca"; m.textContent = el.dataset.secao;
      if (daSecao(el.dataset.secao).length) m.insertAdjacentHTML("beforeend", `<small>${esc(escolhasDe(el.dataset.secao))}</small>`);
      m.setAttribute("aria-label", `Refinar a seção ${el.dataset.secao}`);
      m.addEventListener("click", (ev) => { ev.preventDefault(); ev.stopPropagation(); abrir(el.dataset.secao); });
      el.prepend(m);
    });
  }

  /* ------------------------------------------------------------ alfinetes */
  /* uma seção pode ter mais de um elemento marcado (ex.: faixa + bloco no Ritmo):
     o alfinete guarda qual deles (`parte`) */
  const partesDe = (doc, n) => [...doc.querySelectorAll(`[data-secao="${n}"]`)];
  const secaoDoAlvo = (alvo) => {
    const estilo = (el) => el.ownerDocument.defaultView.getComputedStyle(el);
    let el = alvo.closest("[data-secao]");
    while (el && (!el.offsetHeight || estilo(el).position === "fixed")) el = el.parentElement?.closest("[data-secao]");
    return el;
  };
  const vistaDe = (win) => (win.innerWidth <= 767 ? "mobile" : "desktop");
  const vistaDoPin = (a) => a.vista ?? "desktop";
  const embutido = (() => { try { return !!window.frameElement?.closest(".rf-visor"); } catch { return false; } })();
  if (embutido) addEventListener("message", (e) => {
    const { rf, chave, valor } = e.data ?? {};
    if (e.origin !== location.origin || rf !== "variante" || !VARIANTES[chave]) return;
    secao(VARIANTES[chave].secao).variantes[chave] = valor;
    aplicar(chave);
  });
  const quadroAtual = () => { const f = visor?.querySelector("iframe"); return V.aberto && f?.dataset.pronto && f.contentDocument ? f : null; };
  function descrever(el) {
    const rotulo = el.getAttribute("aria-label") || el.getAttribute("alt") || el.textContent?.trim().replace(/\s+/g, " ");
    const tipo = { BUTTON: "botão", A: "link", IMG: "imagem", H1: "título", H2: "título", H3: "título", P: "texto", INPUT: "campo", LI: "item" }[el.tagName] ?? el.tagName.toLowerCase();
    return rotulo ? `${tipo} “${rotulo.slice(0, 48)}${rotulo.length > 48 ? "…" : ""}”` : tipo;
  }
  function alternarAlfinetar(v = !ui.alfinetar) {
    ui.alfinetar = v;
    html.classList.toggle("rf-alfinetando", v);
    quadroAtual()?.contentDocument.documentElement.classList.toggle("rf-alfinetando", v);
    painel?.querySelectorAll('[data-acao="alfinete"]').forEach((b) => b.setAttribute("aria-pressed", String(v)));
    if (v) avisar(`${rotuloDe(VA)} · toque no ponto (Esc cancela)`);
  }
  function aoClicarAlfinetando(e) {
    if (!ui.alfinetar || e.target.closest(".rf-painel, .rf-botao, .rf-pop, .rf-visor-barra")) return;
    e.preventDefault(); e.stopPropagation();
    const sec = secaoDoAlvo(e.target);
    if (!sec) { avisar("Não dá para fixar aqui"); return; }
    const r = sec.getBoundingClientRect();
    const doc = sec.ownerDocument, win = doc.defaultView;
    const a = { id: Math.random().toString(36).slice(2, 8), versao: VA, secao: sec.dataset.secao, parte: partesDe(doc, sec.dataset.secao).indexOf(sec), x: +((e.clientX - r.left) / r.width).toFixed(4), y: +((e.clientY - r.top) / r.height).toFixed(4), vista: vistaDe(win), alvo: descrever(e.target), texto: "", rodada: estado.rodada.numero, em: agora(), resolvido: false };
    estado.alfinetes.push(a);
    alternarAlfinetar(false);
    if (innerWidth <= 767 && ui.layout === "a" && !ui.mini) minimizar(true);
    desenhar(); salvar();
    abrirPop(a.id);
  }
  function desenharAlfinetes() {
    if (embutido) return;
    desenharAlfinetesEm(document);
    const f = quadroAtual(); if (f) desenharAlfinetesEm(f.contentDocument);
  }
  function desenharAlfinetesEm(doc) {
    const win = doc.defaultView;
    doc.querySelectorAll(".rf-pin").forEach((p) => p.remove());
    if (painel.hidden) return;
    estado.alfinetes.forEach((a) => {
      if (a.versao !== VA || a.resolvido || vistaDoPin(a) !== vistaDe(win)) return;
      const sec = partesDe(doc, a.secao)[a.parte ?? 0] ?? partesDe(doc, a.secao)[0]; if (!sec) return;
      if (win.getComputedStyle(sec).position === "static") sec.style.position = "relative";
      const p = doc.createElement("button");
      p.type = "button"; p.className = "rf-pin"; p.dataset.pin = a.id;
      p.textContent = alfinetesDaVersao().indexOf(a) + 1;
      p.style.left = `${a.x * 100}%`; p.style.top = `${a.y * 100}%`;
      p.title = a.texto || a.alvo;
      p.addEventListener("click", (ev) => { ev.preventDefault(); ev.stopPropagation(); abrirPop(a.id); });
      sec.append(p);
    });
  }
  function acharPin(id) {
    const aqui = document.querySelector(`.rf-pin[data-pin="${id}"]`);
    if (aqui) return { el: aqui, dx: 0, dy: 0 };
    const f = quadroAtual(), el = f?.contentDocument.querySelector(`.rf-pin[data-pin="${id}"]`);
    if (!el) return null;
    const r = f.getBoundingClientRect();
    return { el, dx: r.left, dy: r.top };
  }
  async function abrirPop(id) {
    fecharPop();
    const a = estado.alfinetes.find((x) => x.id === id); if (!a || a.versao !== VA) return;
    /* alfinete da vista mobile: abre o visor mobile */
    if (!acharPin(id) && !a.resolvido && vistaDoPin(a) !== vistaDe(window) && vistaDoPin(a) === "mobile") {
      if (!V.aberto) { await abrirVisor({ n: a.secao }); desenharAlfinetes(); }
    }
    acharPin(id)?.el.scrollIntoView({ block: "center", behavior: calmo() ? "auto" : "smooth" });
    document.querySelectorAll(".rf-pin.ativo").forEach((p) => p.classList.remove("ativo"));
    quadroAtual()?.contentDocument.querySelectorAll(".rf-pin.ativo").forEach((p) => p.classList.remove("ativo"));
    acharPin(id)?.el.classList.add("ativo");
    pop = document.createElement("div");
    pop.className = "rf-pop";
    pop.innerHTML = `<span>${esc(rotuloDe(a.versao))} · ${a.secao} · ${esc(a.alvo)}</span><textarea rows="3" aria-label="Comentário do alfinete" placeholder="Comentário">${esc(a.texto)}</textarea>
      <div><button type="button" data-pop="ok">Pronto</button><button type="button" data-pop="resolver">${a.resolvido ? "Reabrir" : "Resolver"}</button><button type="button" data-pop="apagar">Apagar</button></div>`;
    document.body.append(pop);
    const posicionar = () => { const p = acharPin(id); if (!p) return; const r = p.el.getBoundingClientRect();
      if (innerWidth <= 767) {
        const y = r.bottom + p.dy + 10, alto = pop.offsetHeight || 180;
        pop.style.left = `${Math.max(12, (innerWidth - pop.offsetWidth) / 2)}px`;
        pop.style.top = `${y + alto < innerHeight - 70 ? y : Math.max(12, r.top + p.dy - alto - 38)}px`;
        return;
      }
      pop.style.left = `${Math.min(innerWidth - 300, Math.max(12, r.left + p.dx + 20))}px`; pop.style.top = `${Math.min(innerHeight - 190, Math.max(12, r.top + p.dy - 10))}px`; };
    setTimeout(posicionar, calmo() ? 0 : 350); posicionar();
    const ta = pop.querySelector("textarea"); ta.focus();
    ta.addEventListener("input", () => { a.texto = ta.value; salvar(); });
    pop.addEventListener("click", (e) => {
      const acao = e.target.dataset.pop; if (!acao) return;
      if (acao === "resolver") a.resolvido = !a.resolvido;
      if (acao === "apagar") estado.alfinetes = estado.alfinetes.filter((x) => x.id !== id);
      fecharPop(); desenhar(); salvar();
    });
  }
  function fecharPop() {
    pop?.remove(); pop = null;
    document.querySelectorAll(".rf-pin.ativo").forEach((p) => p.classList.remove("ativo"));
    try { quadroAtual()?.contentDocument.querySelectorAll(".rf-pin.ativo").forEach((p) => p.classList.remove("ativo")); } catch {}
  }

  /* ------------------------------------------------------------ rodadas: fechar congela todas as versões */
  async function fecharRodada() {
    let commit = null;
    try { commit = await (await fetch("/__lab/git", { cache: "no-store" })).json(); } catch {}
    const n = estado.rodada.numero;
    /* a rodada nova começa limpa: conversas abertas vão para o Arquivo */
    for (const [v, x] of Object.entries(estado.versoes)) for (const id of Object.keys(x.secoes ?? {})) arquivarConversa(v, id, "rodada");
    arquivarConversa(VA, "G", "rodada");
    estado.rodadas.push({ numero: n, aberta_em: estado.rodada.aberta_em, fechada_em: agora(), commit, geral: estado.geral, versoes: JSON.parse(JSON.stringify(estado.versoes)), alfinetes: JSON.parse(JSON.stringify(estado.alfinetes)) });
    for (const v of Object.values(estado.versoes)) {
      for (const s of Object.values(v.secoes ?? {})) {
        if (s.nota?.trim()) s.pedido = { rodada: n, nota: s.nota.trim() };
        s.nota = "";
      }
    }
    estado.pedidoGeral = estado.geral?.trim() ? { rodada: n, texto: estado.geral.trim() } : null;
    estado.geral = "";
    estado.pedidoDeRevisao = { rodada: n, em: agora(), atendido: false };
    estado.rodada = { numero: n + 1, aberta_em: agora() };
    desenhar(); salvar();
    avisar(`R${n} fechada. Agora é a R${n + 1}.`);
  }

  /* ------------------------------------------------------------ visor mobile (390 px) */
  const V = { aberto: false };
  function secaoEmVista(doc = document, win = window) {
    const els = [...doc.querySelectorAll("[data-secao]")].filter((el) => el.offsetHeight && win.getComputedStyle(el).position !== "fixed");
    let melhor = null;
    for (const el of els) { const r = el.getBoundingClientRect(); if (r.top <= win.innerHeight * 0.35) melhor = el; }
    return { n: melhor?.dataset.secao, fracao: (win.scrollY || 0) / Math.max(1, doc.documentElement.scrollHeight - win.innerHeight) };
  }
  function levarPara(win, doc, alvo) {
    const el = alvo.n && doc.querySelector(`[data-secao="${alvo.n}"]`);
    doc.documentElement.style.scrollBehavior = "auto";
    const ir = () => {
      if (el) win.scrollTo(0, el.getBoundingClientRect().top + win.scrollY - 40);
      else win.scrollTo(0, (alvo.fracao ?? 0) * (doc.documentElement.scrollHeight - win.innerHeight));
    };
    ir(); setTimeout(ir, 400); setTimeout(ir, 1200);
  }
  async function abrirVisor({ n } = {}) {
    const alvo = n ? { n } : secaoEmVista();
    V.aberto = true;
    if (!visor) montarVisor();
    visor.hidden = false; html.classList.add("rf-com-visor");
    desenhar();
    const f = visor.querySelector("iframe");
    f.dataset.pronto = "";
    await new Promise((ok) => { f.onload = ok; f.src = location.pathname + location.search; });
    const d = f.contentDocument;
    d.head.insertAdjacentHTML("beforeend", "<style>.rf-botao,.rf-painel{display:none!important}</style>");
    d.addEventListener("click", aoClicarAlfinetando, true);
    d.defaultView.addEventListener("keydown", (e) => { if (e.key === "Escape" && ui.alfinetar) alternarAlfinetar(false); });
    d.defaultView.addEventListener("resize", () => desenharAlfinetesEm(d));
    try { await d.fonts?.ready; } catch {}
    await new Promise((ok) => setTimeout(ok, 400));
    levarPara(d.defaultView, d, alvo);
    f.dataset.pronto = "1"; desenharAlfinetes();
  }
  function montarVisor() {
    visor = document.createElement("div");
    visor.className = "rf-visor"; visor.hidden = true;
    visor.innerHTML = `<div class="rf-visor-palco"><iframe title="Versão mobile"></iframe></div>
      <div class="rf-visor-barra"><span>${esc(rotuloDe(VA))} · 390</span><button type="button" class="rf-x" data-v-fechar title="Fechar  V" aria-label="Fechar o visor mobile">${ico("fechar")}</button></div>`;
    document.body.append(visor);
    visor.addEventListener("click", (e) => { if (e.target.closest("[data-v-fechar]")) fecharVisor(); });
  }
  function fecharVisor() {
    if (!visor) return;
    V.aberto = false; visor.hidden = true; html.classList.remove("rf-com-visor");
    visor.querySelector("iframe").src = "about:blank";
    desenhar();
  }

  /* ------------------------------------------------------------ abrir, fechar, lado, mini, layout */
  function abrir(n) {
    painel.hidden = false; botao.hidden = true; guarda.gravar(`${CHAVE}-aberto`, "1");
    if (ui.mini) { ui.mini = false; guarda.gravar(`${CHAVE}-mini`, "0"); }
    ui.emVista = secaoEmVista().n ?? (UMA ? null : "0");
    if (n) { ui.foco = n; ui.vista = "secoes"; if (ui.layout === "c") ui.cartao = "sec"; if (ui.layout === "b") ui.gaveta = "nota"; }
    else if (ui.seguir && ui.emVista) ui.foco = ui.emVista;
    desenhar();
    if (n || ui.foco) { mostrarNaLista(`[data-linha="${ui.foco}"]`); if (!DEMO) painel.querySelector(`[data-nota-de="${n}"]`)?.focus({ preventScroll: true }); }
  }
  function fechar() { painel.hidden = true; botao.hidden = false; guarda.gravar(`${CHAVE}-aberto`, "0"); fecharPop(); alternarAlfinetar(false); desenharAlfinetes(); }
  function minimizar(v = !ui.mini) { ui.mini = v; guarda.gravar(`${CHAVE}-mini`, v ? "1" : "0"); desenhar(); }
  function mudarLado(para) {
    ui.lado = para ?? (ui.lado === "direita" ? "esquerda" : "direita");
    guarda.gravar(`${CHAVE}-lado`, ui.lado); painel.dataset.lado = ui.lado;
  }
  function mudarLayout(id) {
    const i = LAYOUTS.findIndex(([x]) => x === ui.layout);
    ui.layout = id ?? LAYOUTS[(i + 1) % LAYOUTS.length][0];
    guarda.gravar("refinamento-layout", ui.layout);
    ui.mapa = false; ui.ajuda = false; ui.confirmar = false; ui.vista = "secoes"; ui.gaveta = null; ui.conversas.clear();
    ui.cartao = ui.layout === "c" && ui.foco ? "sec" : null;
    desenhar();
    avisar(`Painel ${ui.layout.toUpperCase()} · ${LAYOUTS.find(([x]) => x === ui.layout)[1]}`);
  }
  async function irAoIndice() { await salvarJa(); location.href = `${prefixo}indice/`; }
  async function irAoSistema() { await salvarJa(); location.href = `${prefixo}lab/sistema/`; }
  const alternarNumerar = () => { ui.numerar = !ui.numerar; guarda.gravar(`${CHAVE}-numerar`, ui.numerar ? "1" : "0"); if (!painel.hidden) desenhar(); else numerar(); };
  function alternarSeguir() {
    ui.seguir = !ui.seguir; guarda.gravar("refinamento-seguir", ui.seguir ? "1" : "0");
    desenhar(); avisar(ui.seguir ? "Acompanhando a rolagem" : "Painel parado na seção escolhida");
  }

  /* ------------------------------------------------------------ ações */
  const ACOES = {
    fechar, lado: () => mudarLado(), mini: () => minimizar(), layout: (arg) => mudarLayout(arg),
    alfinete: () => alternarAlfinetar(), mobile: () => (V.aberto ? fecharVisor() : abrirVisor()), numerar: alternarNumerar, seguir: alternarSeguir,
    mapa: () => { ui.mapa = !ui.mapa; if (ui.mapa) carregarResumo(); desenhar(); },
    ajuda: () => { ui.ajuda = !ui.ajuda; desenhar(); },
    arquivo: () => { ui.vista = ui.vista === "arquivo" ? "secoes" : "arquivo"; ui.confirmar = false; desenhar(); },
    indice: irAoIndice,
    sistema: irAoSistema,
    /* aprovação: aprovar soma o que está na tela; ajustar alterna; reabrir volta a "em aberto" */
    aprovar: (n) => { aprovar(n); salvar(); desenhar(); avisar(`${n} · aprovada${escolhasDe(n) ? `: ${escolhasDe(n)}` : ""}`); },
    ajustar: (n) => { const s = secao(n); s.status = s.status === "ajustar" ? "revisar" : "ajustar"; salvar(); desenhar(); },
    reabrir: (n) => { reabrir(n); salvar(); desenhar(); },
    principal: (arg) => { const [n, k, id] = arg.split("|"); tornarPrincipal(n, k, id); escolherVariante(k, id); },
    tirar: (arg) => { const [n, k, id] = arg.split("|"); tirarAprovada(n, k, id); salvar(); desenhar(); },
    /* conversas */
    conversa: (id) => { ui.conversas.has(id) ? ui.conversas.delete(id) : ui.conversas.add(id); desenhar(); },
    resolver: (id) => { arquivarConversa(VA, id, "resolvida"); ui.conversas.delete(id); salvar(); desenhar(); avisar("Conversa no Arquivo"); },
    "reabrir-conversa": (i) => { reabrirConversa(Number(i)); salvar(); desenhar(); },
    /* Arquivo: ver uma opção antiga na página */
    ver: (arg) => { const [n, k, id] = arg.split("|"); escolherVariante(k, id); irParaSecao(n); },
    rodada: () => { ui.confirmar = true; desenhar(); },
    "rodada-nao": () => { ui.confirmar = false; desenhar(); },
    "rodada-sim": () => { ui.confirmar = false; fecharRodada(); },
    /* A: abre/fecha a linha e leva a página até a seção */
    linha: (id) => {
      if (ui.mini) ui.mini = false;
      ui.foco = ui.foco === id ? null : id;
      desenhar();
      if (ui.foco) { mostrarNaLista(`[data-linha="${id}"]`); irParaSecao(id); }
    },
    /* B: gavetas e passo entre seções */
    gaveta: (arg) => { ui.gaveta = ui.gaveta === arg ? null : arg; ui.confirmar = false; if (arg === "mapa") carregarResumo(); desenhar(); if (arg === "nota" && ui.gaveta) painel.querySelector("[data-nota-de]")?.focus({ preventScroll: true }); },
    passo: (arg) => {
      const ls = linhas(), i = ls.findIndex((l) => l.id === focoB());
      const id = ls[Math.max(0, Math.min(ls.length - 1, i + Number(arg)))].id;
      ui.foco = id; desenhar(); irParaSecao(id);
    },
    ir: (id) => { ui.foco = id; ui.gaveta = null; desenhar(); irParaSecao(id); },
    /* C: número do trilho abre o cartão da seção */
    trilho: (id) => {
      if (ui.cartao === "sec" && ui.foco === id) ui.cartao = null;
      else { ui.foco = id; ui.cartao = "sec"; irParaSecao(id); }
      ui.confirmar = false; desenhar();
    },
    cartao: (arg) => { ui.cartao = ui.cartao === arg ? null : arg; ui.confirmar = false; if (arg === "mapa") carregarResumo(); desenhar(); },
  };

  /* ------------------------------------------------------------ montagem */
  function montar() {
    if (dentroDoVisor) return;
    botao = document.createElement("button");
    botao.type = "button"; botao.className = "rf-botao";
    botao.title = "Laboratório (R)";
    botao.innerHTML = `<span>Lab</span><em>${esc(rotuloDe(VA))}</em><b></b>`;
    painel = document.createElement("aside");
    painel.className = "rf-painel"; painel.hidden = true; painel.setAttribute("aria-label", "Laboratório");
    document.body.append(botao, painel);

    botao.addEventListener("click", () => abrir());
    painel.addEventListener("click", (e) => {
      const t = e.target;
      const mp = t.closest("[data-mapa-ir]");
      if (mp) { e.preventDefault(); if (mp.getAttribute("aria-current")) return; salvarJa().then(() => { location.href = mp.href; }); return; }
      const ac = t.closest("[data-acao]"); if (ac) return ACOES[ac.dataset.acao]?.(ac.dataset.arg);
      const ex = t.closest("[data-expandir]"); if (ex) { ex.classList.toggle("inteiro"); return; }
      const vp = t.closest("[data-ver-pin]"); if (vp) return abrirPop(vp.dataset.verPin);
      const v = t.closest("[data-var]"); if (v) return escolherVariante(v.dataset.var, v.dataset.valor);
    });
    painel.addEventListener("toggle", (e) => e.stopPropagation(), true);
    painel.addEventListener("input", (e) => {
      const id = e.target.dataset?.notaDe; if (id == null) return;
      if (id === "G") estado.geral = e.target.value; else secao(id).nota = e.target.value;
      salvar();
    });
    /* sair do campo: redesenha para os sinais (lápis) acompanharem */
    painel.addEventListener("focusout", (e) => { if (e.target.matches?.("[data-nota-de]")) setTimeout(() => { if (!digitando()) desenhar(); }, 0); });
    document.addEventListener("click", aoClicarAlfinetando, true);
    document.addEventListener("pointerdown", (e) => { if (pop && !e.target.closest(".rf-pop, .rf-pin")) fecharPop(); });

    addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        if (ui.alfinetar) return alternarAlfinetar(false);
        if (pop) return fecharPop();
        if (V.aberto) return fecharVisor();
        if (ui.confirmar) { ui.confirmar = false; return desenhar(); }
        if (ui.mapa) { ui.mapa = false; return desenhar(); }
        if (ui.gaveta) { ui.gaveta = null; return desenhar(); }
        if (ui.cartao) { ui.cartao = null; return desenhar(); }
        if (!painel.hidden) return fechar();
        return;
      }
      if (e.metaKey || e.ctrlKey || e.altKey || digitando()) return;
      const k = e.key.toLowerCase();
      /* produto-ports-android: as setas são o direcional do app; aqui é Shift + ←/→ */
      if (e.shiftKey && (e.key === "ArrowRight" || e.key === "ArrowLeft")) { e.preventDefault(); return trocarVariacaoEmVista(e.key === "ArrowRight" ? 1 : -1); }
      if (k === "r") return painel.hidden ? abrir() : fechar();
      if (k === "i") return irAoIndice();
      if (k === "d") return irAoSistema();
      if (k === "l") { if (painel.hidden) abrir(); return mudarLayout(); }
      if (k === "m") { if (painel.hidden) return abrir(); if (ui.layout === "a") minimizar(); else { ui.gaveta = null; ui.cartao = null; desenhar(); } return; }
      if (k === "[") return mudarLado("esquerda");
      if (k === "]") return mudarLado("direita");
      if (k === "p") { if (painel.hidden) abrir(); return alternarAlfinetar(); }
      if (k === "v") return V.aberto ? fecharVisor() : abrirVisor();
      if (k === "n") return alternarNumerar();
      if (k === "s") return alternarSeguir();
    });

    if (DEMO) {
      Object.assign(ui, { layout: DEMO, mini: false, numerar: false, lado: "direita" });
      const n = busca.get("lab-secao");
      if (n) { levarPara(window, document, { n }); ui.emVista = n; ui.foco = n; }
      abrir(n || undefined);
      if (DEMO === "c" && ui.foco) { ui.cartao = "sec"; desenhar(); }
      if (DEMO === "b") { ui.gaveta = null; desenhar(); }
      return;
    }
    if (guarda.ler(`${CHAVE}-aberto`, "0") === "1") abrir(); else desenhar();
    /* chegou de outra página com uma seção em vista: volta para ela */
    const ir = sessao.ler(`${CHAVE}-ir`);
    if (ir) { const ir0 = () => levarPara(window, document, { n: ir }); document.readyState === "complete" ? ir0() : addEventListener("load", ir0, { once: true }); }
    carregar(); carregarResumo();
    setInterval(() => { if (!document.hidden) carregar({ silencioso: true }); }, 8000);
  }

  return { variante, aoMudar, montar, versaoAtual: VA };
}
