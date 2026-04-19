# Testes e CI

O projeto possui validação em três níveis:

- testes de back-end com Spring Boot, MockMvc e H2;
- testes contratuais do front-end com Node;
- E2E real com Playwright.

## Back-end

Windows:

```powershell
cd backend
.\mvnw.cmd test
```

Linux/macOS:

```bash
cd backend
./mvnw test
```

Os testes de back-end usam H2 em memória e não exigem PostgreSQL local.

## Front-end Contratual

Na raiz:

```bash
npm install
npm run test:frontend:contracts
```

Esses testes verificam contratos do cliente `frontend/api.js`, uso da API central, health público, base configurável, refresh automático e integração da tela principal.

## Smoke Visual

```bash
npm run test:frontend:visual
```

Em alguns ambientes locais o Chrome headless pode ser bloqueado. O teste foi escrito para pular com mensagem clara quando isso ocorre.

## Playwright E2E

Instale o navegador:

```bash
npm run playwright:install
```

Rode:

```bash
npm run test:e2e
```

O cenário E2E cobre:

- cadastro/login;
- dashboard;
- navegação por tópico;
- desbloqueio de pré-requisito;
- simulador;
- execução de código inválido;
- execução de código válido;
- histórico de submissões;
- progresso;
- analytics.

## GitHub Actions

Arquivo:

```text
.github/workflows/ci.yml
```

Jobs:

| Job | Sistemas | Objetivo |
|---|---|---|
| `backend` | Linux, Windows, macOS | Executar `mvnw test` |
| `frontend` | Linux, Windows, macOS | Executar contratos do front |
| `e2e` | Linux | Executar Playwright com backend e frontend reais |

O CI usa `chmod +x backend/mvnw` em runners Unix e `mvnw.cmd` no Windows, evitando falhas por permissão do Maven Wrapper.

## Checklist Antes de Abrir PR

- `backend`: testes passando.
- `frontend`: contratos passando.
- `Playwright`: E2E passando localmente ou no CI.
- `.env` fora do commit.
- `node_modules`, `target`, `test-results`, `playwright-report` e `dist` fora do commit.
