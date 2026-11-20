# Build from the repository root, so the gateway and the local MCP server it reuses are both in reach:
#   docker build -f hosted-mcp/Dockerfile -t pmitz-hosted-mcp .
FROM node:22-alpine
ENV NODE_ENV=production
WORKDIR /app/hosted-mcp
COPY hosted-mcp/package.json hosted-mcp/package-lock.json ./
RUN npm ci --omit=dev --no-audit --no-fund
COPY hosted-mcp/server.mjs ./
COPY plugins/pmitz/server/pmitz-mcp.mjs /app/plugins/pmitz/server/pmitz-mcp.mjs
USER node
EXPOSE 8090
CMD ["node", "server.mjs"]
