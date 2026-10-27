/* SERVIDOR DO LABORATÓRIO — um plugin que só existe aqui, a serviço do painel de Refinamento:
   - grava as anotações de cada página em `laboratorio/<pagina>/anotacoes.json`
     (estado) e `ANOTACOES.md` (leitura humana e da IA), e cada rodada fechada
     em `laboratorio/<pagina>/rodadas/R<n>.md`;
   - informa o commit atual (para carimbar as rodadas);
   - monta versões antigas do laboratório em `laboratorio/_versoes/<hash>/`
     (fora do git) para o visor de antes e depois.
   Nada disso entra no build da loja.
   Rodar da raiz: npm run lab (porta 5192).
   Copiado do lab-modelo-loja (26/10/2026). Mudanças: porta 5192 e o
   antes/depois extrai só `laboratorio/` (aqui não há `store/`). */
import { defineConfig } from "vite";
import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

/* fileURLToPath: a pasta do projeto tem espaço ("Arte Feita") */
const LAB = path.dirname(fileURLToPath(import.meta.url));
const RAIZ = path.resolve(LAB, "..");
const VERSOES = path.join(LAB, "_versoes");
const STATUS = { revisar: "Em revisão", pronta: "Pronta para revisar", aprovada: "Aprovada", ajustar: "Precisa de ajuste", descartar: "Descartar" };

const git = (...args) => execFileSync("git", args, { cwd: RAIZ, stdio: ["ignore", "pipe", "ignore"] }).toString().trim();
const pastaDa = (pagina) => {
  if (!/^[a-z0-9-]+$/.test(pagina ?? "")) throw new Error("página inválida");
  return path.join(LAB, pagina);
};
const lerCorpo = (req) => new Promise((ok, erro) => { let c = ""; req.on("data", (x) => (c += x)); req.on("end", () => ok(c)); req.on("error", erro); });
const responder = (res, dado, status = 200) => { res.statusCode = status; res.setHeader("Content-Type", "application/json"); res.end(JSON.stringify(dado)); };

function secoesEmMarkdown(secoes = {}, alfinetes = [], { atual = true } = {}) {
  const linhas = [];
  /* seção que só tem alfinete não entra em `secoes`: sem isto o alfinete sumia
     do resumo (defeito herdado do laboratório da Bioma, achado no R1 daqui) */
  const todas = { ...Object.fromEntries(alfinetes.map((a) => [a.secao, { status: "revisar", variantes: {} }])), ...secoes };
  for (const [n, s] of Object.entries(todas).sort((a, b) => parseFloat(a[0]) - parseFloat(b[0]))) {
    const vars = Object.entries(s.variantes ?? {}).map(([k, v]) => `${k}: **${v}**`).join(" · ");
    const pins = alfinetes.filter((a) => a.secao === n);
    const temAlgo = s.nota?.trim() || (s.status ?? "revisar") !== "revisar" || vars || pins.length || s.pedido || s.resposta;
    if (!temAlgo) continue;
    linhas.push(`## ${n} · ${s.nome ?? ""}`, "", `Status: **${STATUS[s.status] ?? s.status ?? "Em revisão"}**${vars ? `  \nVariantes vistas: ${vars}` : ""}`, "");
    if (atual && s.pedido?.nota) linhas.push(`> **Pedido da R${s.pedido.rodada}:** ${s.pedido.nota.replace(/\n/g, "\n> ")}`, "");
    if (atual && s.resposta?.texto) linhas.push(`> **Resposta da IA${s.resposta.rodada ? ` (R${s.resposta.rodada})` : ""}:** ${s.resposta.texto.replace(/\n/g, "\n> ")}`, "");
    if (s.nota?.trim()) linhas.push(s.nota.trim(), "");
    for (const a of pins) linhas.push(`- 📍 ${a.vista === "mobile" ? "**[mobile]** " : ""}${a.resolvido ? "~~" : ""}${a.alvo} (x ${Math.round(a.x * 100)}%, y ${Math.round(a.y * 100)}% da seção): ${a.texto?.trim() || "sem texto"}${a.resolvido ? "~~ (resolvido)" : ""}`);
    if (pins.length) linhas.push("");
  }
  return linhas;
}

function emMarkdown(d, pagina) {
  const r = d.rodada ?? { numero: 1 };
  const linhas = [`# Refinamento — ${pagina} · rodada R${r.numero}`, "", `Gerado pelo painel de Refinamento em ${d.atualizado ?? "—"}. Não editar à mão: o painel regrava este arquivo. A IA responde em \`anotacoes.json\` (\`secoes[n].resposta\` + status \`pronta\`).`, ""];
  if (d.pedidoDeRevisao && !d.pedidoDeRevisao.atendido) linhas.push(`**Revisão pedida à IA** em ${d.pedidoDeRevisao.em} (pedidos da R${d.pedidoDeRevisao.rodada}).`, "");
  if (d.pedidoGeral?.texto) linhas.push(`## Geral · pedido da R${d.pedidoGeral.rodada}`, "", d.pedidoGeral.texto, "");
  if (d.geral?.trim()) linhas.push(`## Geral · R${r.numero}`, "", d.geral.trim(), "");
  linhas.push(...secoesEmMarkdown(d.secoes, d.alfinetes ?? []));
  if (d.rodadas?.length) linhas.push("## Rodadas fechadas", "", ...d.rodadas.map((x) => `- R${x.numero} · ${x.fechada_em} · commit ${x.commit?.curto ?? "—"}${x.commit?.sujo ? "+alterações" : ""} → \`rodadas/R${x.numero}.md\``), "");
  return linhas.join("\n");
}

function rodadaEmMarkdown(x, pagina) {
  const linhas = [`# Refinamento — ${pagina} · R${x.numero} (fechada)`, "", `Aberta em ${x.aberta_em ?? "—"} · fechada em ${x.fechada_em} · commit ${x.commit?.hash ?? "—"}${x.commit?.sujo ? " (com alterações não commitadas)" : ""}`, ""];
  if (x.geral?.trim()) linhas.push("## Geral", "", x.geral.trim(), "");
  linhas.push(...secoesEmMarkdown(x.secoes, x.alfinetes ?? [], { atual: false }));
  return linhas.join("\n");
}

function refinamento() {
  return {
    name: "lab-refinamento",
    configureServer(server) {
      server.middlewares.use("/__lab/anotacoes", async (req, res) => {
        try {
          const pagina = new URL(req.url, "http://x").searchParams.get("pagina") ?? "home";
          const pasta = pastaDa(pagina);
          const arqJson = path.join(pasta, "anotacoes.json");
          if (req.method === "GET") return responder(res, existsSync(arqJson) ? JSON.parse(readFileSync(arqJson, "utf8")) : {});
          if (req.method !== "POST") return responder(res, { ok: false }, 405);
          const d = JSON.parse(await lerCorpo(req));
          d.atualizado = new Date().toISOString();
          writeFileSync(arqJson, JSON.stringify(d, null, 2) + "\n");
          writeFileSync(path.join(pasta, "ANOTACOES.md"), emMarkdown(d, pagina) + "\n");
          for (const x of d.rodadas ?? []) {
            const alvo = path.join(pasta, "rodadas", `R${x.numero}.md`);
            if (existsSync(alvo)) continue;
            mkdirSync(path.dirname(alvo), { recursive: true });
            writeFileSync(alvo, rodadaEmMarkdown(x, pagina) + "\n");
          }
          responder(res, { ok: true, atualizado: d.atualizado });
        } catch (e) { responder(res, { ok: false, erro: String(e) }, 400); }
      });

      server.middlewares.use("/__lab/git", (req, res) => {
        try { responder(res, { hash: git("rev-parse", "HEAD"), curto: git("rev-parse", "--short", "HEAD"), sujo: git("status", "--porcelain", "--", "laboratorio").length > 0 }); }
        catch { responder(res, null); }
      });

      server.middlewares.use("/__lab/versoes", (req, res) => {
        try {
          const pagina = new URL(req.url, "http://x").searchParams.get("pagina") ?? "home";
          pastaDa(pagina);
          const log = git("log", "-n", "20", "--date=format:%d/%m %H:%M", "--format=%H%x09%h%x09%ad%x09%s", "--", `laboratorio/${pagina}`);
          responder(res, log ? log.split("\n").map((l) => { const [hash, curto, data, msg] = l.split("\t"); return { hash, curto, data, msg }; }) : []);
        } catch { responder(res, []); }
      });

      /* extrai `laboratorio/` e `store/` do commit pedido para _versoes/<hash>/ */
      server.middlewares.use("/__lab/versao", (req, res) => {
        try {
          const hash = new URL(req.url, "http://x").searchParams.get("hash") ?? "";
          if (!/^[0-9a-f]{7,40}$/.test(hash)) return responder(res, { ok: false, erro: "hash inválido" }, 400);
          const destino = path.join(VERSOES, hash);
          if (!existsSync(path.join(destino, "laboratorio"))) {
            mkdirSync(destino, { recursive: true });
            const tar = execFileSync("git", ["archive", "--format=tar", hash, "laboratorio"], { cwd: RAIZ, maxBuffer: 1 << 30 });
            execFileSync("tar", ["-x", "-C", destino, "--exclude=laboratorio/_versoes"], { input: tar });
          }
          responder(res, { ok: true });
        } catch (e) { responder(res, { ok: false, erro: String(e) }, 500); }
      });
    },
  };
}

export default defineConfig({
  root: RAIZ,
  plugins: [refinamento()],
  server: { watch: { ignored: ["**/laboratorio/*/anotacoes.json", "**/laboratorio/*/ANOTACOES.md", "**/laboratorio/*/rodadas/**", "**/laboratorio/_versoes/**"] } },
});
