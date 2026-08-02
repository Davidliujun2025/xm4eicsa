#!/usr/bin/env node

import http from "node:http";
import https from "node:https";
import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import path from "node:path";
import { pipeline } from "node:stream";
import { URL } from "node:url";

const args = parseArgs(process.argv.slice(2));
const rootDir = path.resolve(args.root || process.cwd());
const port = Number(args.port || 5179);
const proxyBase = new URL(args.proxyBase || "http://127.0.0.1:8080");
const proxyPaths = ["/api", "/actuator"];
const defaultDocument = "index.html";

const MIME_TYPES = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".txt": "text/plain; charset=utf-8",
  ".woff": "font/woff",
  ".woff2": "font/woff2"
};

const server = http.createServer(async (req, res) => {
  try {
    const requestUrl = new URL(req.url || "/", `http://${req.headers.host || "127.0.0.1"}`);
    if (proxyPaths.some((prefix) => requestUrl.pathname.startsWith(prefix))) {
      proxyRequest(req, res, requestUrl);
      return;
    }

    const filePath = await resolveStaticPath(requestUrl.pathname);
    const extension = path.extname(filePath).toLowerCase();
    res.writeHead(200, {
      "Cache-Control": "no-store",
      "Content-Type": MIME_TYPES[extension] || "application/octet-stream"
    });
    pipeline(createReadStream(filePath), res, (error) => {
      if (error && !res.headersSent) {
        res.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
        res.end("Static file read failed");
      }
    });
  } catch (error) {
    const statusCode = error?.code === "ENOENT" ? 404 : 500;
    res.writeHead(statusCode, { "Content-Type": "text/plain; charset=utf-8" });
    res.end(statusCode === 404 ? "Not found" : "Server error");
  }
});

server.listen(port, "127.0.0.1", () => {
  console.log(`Static dev server ready on http://127.0.0.1:${port}`);
  console.log(`Serving ${rootDir}`);
  console.log(`Proxying API to ${proxyBase.href}`);
});

function parseArgs(argv) {
  const parsed = {};
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (!arg.startsWith("--")) continue;
    const key = arg.slice(2);
    const value = argv[index + 1];
    if (value && !value.startsWith("--")) {
      parsed[key] = value;
      index += 1;
      continue;
    }
    parsed[key] = "true";
  }
  return parsed;
}

async function resolveStaticPath(requestPathname) {
  const decodedPath = decodeURIComponent(requestPathname);
  const relativePath = decodedPath === "/" ? defaultDocument : decodedPath.replace(/^\/+/, "");
  const candidatePath = path.resolve(rootDir, relativePath);

  if (!candidatePath.startsWith(rootDir)) {
    const error = new Error("Path escapes root");
    error.code = "ENOENT";
    throw error;
  }

  try {
    const candidateStat = await stat(candidatePath);
    if (candidateStat.isDirectory()) {
      return path.join(candidatePath, defaultDocument);
    }
    return candidatePath;
  } catch (error) {
    if (path.extname(candidatePath)) {
      throw error;
    }
    return path.join(rootDir, defaultDocument);
  }
}

function proxyRequest(clientReq, clientRes, requestUrl) {
  const transport = proxyBase.protocol === "https:" ? https : http;
  const proxyHeaders = { ...clientReq.headers, host: proxyBase.host };
  delete proxyHeaders.connection;

  const upstreamReq = transport.request(
    {
      protocol: proxyBase.protocol,
      hostname: proxyBase.hostname,
      port: proxyBase.port,
      method: clientReq.method,
      path: `${requestUrl.pathname}${requestUrl.search}`,
      headers: proxyHeaders
    },
    (upstreamRes) => {
      clientRes.writeHead(upstreamRes.statusCode || 502, upstreamRes.headers);
      pipeline(upstreamRes, clientRes, () => {});
    }
  );

  upstreamReq.on("error", () => {
    if (!clientRes.headersSent) {
      clientRes.writeHead(502, { "Content-Type": "text/plain; charset=utf-8" });
    }
    clientRes.end("Upstream request failed");
  });

  pipeline(clientReq, upstreamReq, () => {});
}
