/* SERVIDOR DO LABORATÓRIO — plugin do Vite com os endpoints do painel de
   Refinamento do portfólio (victor-moraes-site/laboratorio/integracao.mjs,
   copiado em 27/10/2026; lá é uma integração do Astro, aqui é plugin do Vite):
   - /__lab/anotacoes?pagina=<tela>  GET/POST `laboratorio/<tela>/anotacoes.json`,
     regrava `ANOTACOES.md` e cada rodada fechada em `rodadas/R<n>.md`;
   - /__lab/resumo?paginas=a,b  estado de revisão de várias telas (menu de páginas);
   - /__lab/git  commit atual (carimbo das rodadas);
   - /indice/ e /lab/sistema/  levam ao índice do lab.
   Rodar da raiz: npm run lab (porta 5192). */
import { defineConfig } from "vite";
import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const LAB = path.dirname(fileURLToPath(import.meta.url));
const RAIZ = path.resolve(LAB, "..");
const STATUS = { revisar: "Em aberto", pronta: "IA respondeu", aprovada: "Aprovada", ajustar: "Precisa de ajuste", descartar: "Descartar" };

const git = (...args) => execFileSync("git", args, { cwd: RAIZ, stdio: ["ignore", "pipe", "ignore"] }).toString().trim();
const pastaDa = (pagina) => {
  if (!/^[a-z0-9-]+$/.test(pagina ?? "")) throw new Error("página inválida");
  return path.join(LAB, pagina);
};
const lerCorpo = (req) => new Promise((ok, erro) => { let c = ""; req.on("data", (x) => (c += x)); req.on("end", () => ok(c)); req.on("error", erro); });
const responder = (res, dado, status = 200) => { res.statusCode = status; res.setHeader("Content-Type", "application/json"); res.end(JSON.stringify(dado)); };
const citar = (s) => s.replace(/\n/g, "\n> ");

function secoesEmMarkdown(secoes = {}, alfinetes = [], nomes = {}, { atual = true } = {}) {
  const linhas = [];
  for (const [n, s] of Object.entries(secoes).sort((a, b) => parseFloat(a[0]) - parseFloat(b[0]))) {
    const vars = Object.entries(s.variantes ?? {}).map(([k, v]) => `${k}: **${v}**`).join(" · ");
    const pins = alfinetes.filter((a) => a.secao === n);
    const temAlgo = s.nota?.trim() || (s.status ?? "revisar") !== "revisar" || vars || pins.length || s.pedido || s.resposta;
    if (!temAlgo) continue;
    const nome = n === "0" ? "Geral da versão" : nomes[n] ?? s.nome ?? "";
    /* aprovadas: a primeira de cada chave é a principal (a que vai para o site) */
    const aprovadas = Object.entries(s.aprovadas ?? {}).map(([k, l]) => `${k}: ${l.map((x, i) => (i ? x : `**${x}** (principal)`)).join(", ")}`).join(" · ");
    linhas.push(`### ${n} · ${nome}`, "", `Status: **${STATUS[s.status] ?? s.status ?? "Em revisão"}**${aprovadas ? `  \nAprovadas: ${aprovadas}` : ""}${vars ? `  \nVariantes vistas: ${vars}` : ""}`, "");
    if (atual && s.pedido?.nota) linhas.push(`> **Pedido da R${s.pedido.rodada}:** ${citar(s.pedido.nota)}`, "");
    if (atual && s.resposta?.texto) linhas.push(`> **Resposta da IA${s.resposta.rodada ? ` (R${s.resposta.rodada})` : ""}:** ${citar(s.resposta.texto)}`, "");
    if (s.nota?.trim()) linhas.push(s.nota.trim(), "");
    for (const a of pins) linhas.push(`- 📍 ${a.vista === "mobile" ? "**[mobile]** " : ""}${a.resolvido ? "~~" : ""}${a.alvo} (x ${Math.round(a.x * 100)}%, y ${Math.round(a.y * 100)}% da seção${a.parte ? `, parte ${a.parte + 1}` : ""}): ${a.texto?.trim() || "sem texto"}${a.resolvido ? "~~ (resolvido)" : ""}`);
    if (pins.length) linhas.push("");
  }
  return linhas;
}

function versoesEmMarkdown(d, { atual = true } = {}) {
  const meta = d.meta ?? {};
  const nomes = Object.fromEntries((meta.secoes ?? []).map((s) => [s.n, s.nome]));
  const ordem = (meta.versoes ?? []).map((v) => v.id);
  const ids = [...new Set([...ordem, ...Object.keys(d.versoes ?? {})])];
  const linhas = [];
  for (const id of ids) {
    const v = d.versoes?.[id] ?? { secoes: {} };
    const info = (meta.versoes ?? []).find((x) => x.id === id);
    const alfinetes = (d.alfinetes ?? []).filter((a) => a.versao === id);
    /* cada página pode ter as próprias seções */
    const nomesV = info?.secoes ? Object.fromEntries(info.secoes.map((s) => [s.n, s.nome])) : nomes;
    /* garante que seções só com alfinetes apareçam */
    const secoes = { ...(v.secoes ?? {}) };
    for (const a of alfinetes) secoes[a.secao] ??= { status: "revisar" };
    const corpo = secoesEmMarkdown(secoes, alfinetes, nomesV, { atual });
    const rota = info ? `/${info.rota}` : "";
    linhas.push(`## Versão: ${info?.rotulo ?? id}${rota ? ` · \`${rota}\`` : ""}${info && !info.disponivel ? " · em breve" : ""}`, "");
    linhas.push(...(corpo.length ? corpo : ["Sem anotações.", ""]));
  }
  return linhas;
}

function emMarkdown(d, pagina) {
  const r = d.rodada ?? { numero: 1 };
  const linhas = [`# Laboratório — ${pagina} · rodada R${r.numero}`, "", `Gerado pelo painel em ${d.atualizado ?? "—"}. Não editar à mão: o painel regrava este arquivo. Cada comentário pertence à versão do cabeçalho em que está. A IA responde em \`anotacoes.json\` (\`versoes[v].secoes[n].resposta\` + status \`pronta\`). Ver laboratorio/README.md.`, ""];
  if (d.pedidoDeRevisao && !d.pedidoDeRevisao.atendido) linhas.push(`**Revisão pedida à IA** em ${d.pedidoDeRevisao.em} (pedidos da R${d.pedidoDeRevisao.rodada}).`, "");
  const geral = [];
  if (d.pedidoGeral?.texto) geral.push(`> **Pedido da R${d.pedidoGeral.rodada}:** ${citar(d.pedidoGeral.texto)}`, "");
  if (d.respostaGeral?.texto) geral.push(`> **Resposta da IA:** ${citar(d.respostaGeral.texto)}`, "");
  if (d.geral?.trim()) geral.push(d.geral.trim(), "");
  if (geral.length) linhas.push("## Geral · todas as versões", "", ...geral);
  linhas.push(...versoesEmMarkdown(d));
  if (d.rodadas?.length) linhas.push("## Rodadas fechadas", "", ...d.rodadas.map((x) => `- R${x.numero} · ${x.fechada_em} · commit ${x.commit?.curto ?? "—"}${x.commit?.sujo ? "+alterações" : ""} → \`rodadas/R${x.numero}.md\``), "");
  return linhas.join("\n");
}

function rodadaEmMarkdown(x, pagina, meta) {
  const linhas = [`# Laboratório — ${pagina} · R${x.numero} (fechada)`, "", `Aberta em ${x.aberta_em ?? "—"} · fechada em ${x.fechada_em} · commit ${x.commit?.hash ?? "—"}${x.commit?.sujo ? " (com alterações não commitadas)" : ""}`, ""];
  if (x.geral?.trim()) linhas.push("## Geral · todas as versões", "", x.geral.trim(), "");
  linhas.push(...versoesEmMarkdown({ versoes: x.versoes, alfinetes: x.alfinetes, meta }, { atual: false }));
  return linhas.join("\n");
}


function laboratorio() {
  return {
    name: "laboratorio",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const caminho = (req.url ?? "").split("?")[0];
        if (!/^\/(pt\/)?(indice|lab\/sistema)\/?$/.test(caminho)) return next();
        res.statusCode = 302; res.setHeader("Location", "/laboratorio/index.html"); res.end();
      });
        server.middlewares.use("/__lab/anotacoes", async (req, res) => {
          try {
            const pagina = new URL(req.url, "http://x").searchParams.get("pagina") ?? "home";
            const pasta = pastaDa(pagina);
            const arqJson = path.join(pasta, "anotacoes.json");
            if (req.method === "GET") return responder(res, existsSync(arqJson) ? JSON.parse(readFileSync(arqJson, "utf8")) : {});
            if (req.method !== "POST") return responder(res, { ok: false }, 405);
            const d = JSON.parse(await lerCorpo(req));
            const meta = d.meta; delete d.meta;
            d.atualizado = new Date().toISOString();
            mkdirSync(pasta, { recursive: true });
            writeFileSync(arqJson, JSON.stringify(d, null, 2) + "\n");
            writeFileSync(path.join(pasta, "ANOTACOES.md"), emMarkdown({ ...d, meta }, pagina) + "\n");
            for (const x of d.rodadas ?? []) {
              const alvo = path.join(pasta, "rodadas", `R${x.numero}.md`);
              if (existsSync(alvo)) continue;
              mkdirSync(path.dirname(alvo), { recursive: true });
              writeFileSync(alvo, rodadaEmMarkdown(x, pagina, meta) + "\n");
            }
            responder(res, { ok: true, atualizado: d.atualizado });
          } catch (e) { responder(res, { ok: false, erro: String(e) }, 400); }
        });
        /* estado de revisão de várias páginas de uma vez (o menu de páginas do painel) */
        server.middlewares.use("/__lab/resumo", (req, res) => {
          try {
            const paginas = (new URL(req.url, "http://x").searchParams.get("paginas") ?? "").split(",").filter(Boolean);
            const saida = {};
            for (const p of paginas) {
              const arq = path.join(pastaDa(p), "anotacoes.json");
              const d = existsSync(arq) ? JSON.parse(readFileSync(arq, "utf8")) : {};
              saida[p] = Object.fromEntries(Object.entries(d.versoes ?? {}).map(([v, x]) => [v, {
                secoes: Object.fromEntries(Object.entries(x.secoes ?? {}).map(([n, s]) => [n, s.status ?? "revisar"])),
                pins: (d.alfinetes ?? []).filter((a) => a.versao === v && !a.resolvido).length,
              }]));
            }
            responder(res, saida);
          } catch (e) { responder(res, { erro: String(e) }, 400); }
        });
        server.middlewares.use("/__lab/git", (req, res) => {
          try { responder(res, { hash: git("rev-parse", "HEAD"), curto: git("rev-parse", "--short", "HEAD"), sujo: git("status", "--porcelain").length > 0 }); }
          catch { responder(res, null); }
        });
    },
  };
}

export default defineConfig({
  root: RAIZ,
  plugins: [laboratorio()],
  server: { watch: { ignored: ["**/laboratorio/*/anotacoes.json", "**/laboratorio/*/ANOTACOES.md", "**/laboratorio/*/rodadas/**"] } },
});
