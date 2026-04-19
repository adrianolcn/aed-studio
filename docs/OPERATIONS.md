# Operação Local

Este documento resume os caminhos de execução do AED Studio.

## Caminho Rápido com Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Serviços esperados:

- Front-end: `http://localhost:8081`
- Back-end: `http://localhost:8080`
- Health: `http://localhost:8080/api/health`
- PostgreSQL: `localhost:5432`

O Compose sobe banco, API e front-end. O sandbox usa `CODE_SANDBOX_MODE=local` por padrão para reduzir fricção em ambiente de desenvolvimento. Para demonstração pública, use `CODE_SANDBOX_MODE=docker` e garanta Docker disponível no host do back-end.

Esse caminho não exige Maven instalado na máquina host: o build do back-end acontece dentro do container.

Para validar apenas a sintaxe e a resolução final do Compose:

```bash
npm run compose:config
```

Esse mesmo comando roda no GitHub Actions em Linux.

## Caminho Tradicional

1. Suba PostgreSQL.
2. Configure `.env`.
3. Execute o back-end em `backend/` com Java 17 e Maven.
4. Sirva `frontend/` com Live Server ou servidor estático.

## Troubleshooting

- API não responde: verifique `/api/health` e logs do container `aed-studio-backend`.
- Login falha: confirme `JWT_SECRET`, banco e CORS.
- Front abre, mas dados não carregam: confira a base da API em `frontend/api.js` ou use `?apiBase=http://host:8080`.
- Judge falha: confira `CODE_SANDBOX_MODE`, timeout e imagem Docker.
