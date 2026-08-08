// Local proxy that injects the Authorization header for MCP conformance testing.
// Usage: node mcp-conformance-proxy.js <backend-port> <token>
const http = require('http');
const backendPort = process.argv[2] || 8085;
const token = process.argv[3];

if (!token) {
  console.error('Usage: node mcp-conformance-proxy.js <backend-port> <jwt-token>');
  process.exit(1);
}

const server = http.createServer((req, res) => {
  let body = [];
  req.on('data', (c) => body.push(c));
  req.on('end', () => {
    const payload = Buffer.concat(body);
    const headers = { ...req.headers, authorization: `Bearer ${token}` };
    const proxyReq = http.request(
      { host: 'localhost', port: backendPort, path: req.url, method: req.method, headers },
      (proxyRes) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res);
      }
    );
    proxyReq.on('error', (e) => {
      res.writeHead(502);
      res.end(JSON.stringify({ error: e.message }));
    });
    proxyReq.write(payload);
    proxyReq.end();
  });
});

server.listen(18085, () => {
  console.log('MCP conformance proxy listening on http://localhost:18085 -> :' + backendPort);
});
