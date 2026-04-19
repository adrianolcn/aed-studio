# Testes e CI

O projeto possui validação em três níveis:

- testes de back-end com Spring Boot, MockMvc e H2;
- testes contratuais do front-end com Node;
- E2E real com Playwright.

## Back-end

Windows:

```powershell
cd backend
mvn test
```

Linux/macOS:

```bash
cd backend
mvn test
```

Os testes de back-end usam H2 em memória e não exigem PostgreSQL local. Para execução fora dos containers, o Maven precisa estar instalado no sistema. O caminho via Docker Compose não exige Maven instalado na máquina host, porque a imagem do back-end resolve as dependências dentro do container.

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

O Playwright sobe o back-end com `SPRING_PROFILES_ACTIVE=e2e` por meio de `scripts/run-e2e-backend.js`, evitando parâmetros Maven sensíveis ao shell. O runner usa `spring-boot.run.useTestClasspath=true` por padrão para permitir H2 isolado no perfil E2E. Se o Maven não estiver no `PATH` local, informe o executável por variável de ambiente:

```powershell
$env:MAVEN_EXECUTABLE = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd"
$env:MAVEN_REPO_LOCAL = "C:\dev\aed-studio\.m2\repository"
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
| `compose` | Linux | Validar `docker compose config` |
| `backend` | Linux, Windows, macOS | Executar `mvn -B test` |
| `frontend` | Linux, Windows, macOS | Executar contratos do front |
| `e2e` | Linux | Executar Playwright com backend e frontend reais |

O CI usa Maven diretamente (`mvn -B test`) para funcionar de forma igual em Linux, Windows e macOS. Se o Maven Wrapper for adicionado, padronize todos os comandos em torno dele no mesmo PR.

## Estratégia de Testes

| Camada | Tipo | Objetivo |
|---|---|---|
| Back-end | integração Spring + H2 | validar autenticação, progresso, XP, exercícios, sandbox e contratos centrais |
| Front-end | contrato Node | garantir que a UI continue usando `AedApi`, base configurável e módulos esperados |
| Smoke visual | navegador/headless | detectar tela em branco, overlay travado e quebra grosseira de layout |
| E2E | Playwright | validar fluxo real de aluno com API e front-end |

## Próximos Passos de E2E

- separar specs por domínio (`auth`, `dashboard`, `simulators`, `code-judge`);
- adicionar fixtures de usuário por cenário;
- capturar screenshots em falhas;
- cobrir erro de sandbox, token expirado e API offline;
- manter um fluxo curto obrigatório no CI e fluxos longos como job agendado.

## Checklist Antes de Abrir PR

- `backend`: testes passando.
- `frontend`: contratos passando.
- `Playwright`: E2E passando localmente ou no CI.
- `.env` fora do commit.
- `node_modules`, `target`, `test-results`, `playwright-report` e `dist` fora do commit.
