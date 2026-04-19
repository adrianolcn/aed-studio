# AED Studio - atualização avançada

Esta atualização consolida o AED Studio como uma plataforma educacional integrada para Algoritmos e Estruturas de Dados.

## Principais entregas

- Integração completa front-end/back-end com JWT, refresh token, health check público e base de API configurável.
- Catálogo educacional com trilhas, pré-requisitos, estados de tópico e progresso estruturado.
- Exercícios obrigatórios e exercícios gerados dinamicamente com templates específicos por tópico e persistência.
- Tentativas persistidas, feedback imediato, XP, níveis, streak e badges.
- Simuladores interativos para arrays, pilhas, filas, listas ligadas, BST, hash table e grafos.
- Missões guiadas/desafio de simulador com foco didático, critério formal e validação no back-end.
- Sistema de recomendação com score de risco, revisão por desempenho, revisão espaçada, evidência e atividade sugerida.
- Analytics educacional com acurácia, tentativas, risco, tendência, abandono, melhora/regressão, trilhas e histórico diário de XP.
- Code judge Java com desafios específicos por tópico, dicas, pseudoesqueleto, cenários nomeados, múltiplas assinaturas controladas, modo local para desenvolvimento e modo Docker recomendado para produção.
- Persistência de submissões de código com status, tempo de execução, total/aprovação de testes, histórico por desafio e melhor/última submissão.
- Analytics e recomendações agora consideram desempenho em exercícios de código.
- Playwright E2E real cobrindo cadastro/login, dashboard, tópico, simulador, judge de código, histórico, progresso e analytics.
- Dashboard temático com narrativa original de exploração, mapa de progresso e próximos passos.
- Testes de back-end, contratos de front-end e teste visual E2E com Chrome headless.

## Code judge e sandbox

Para produção ou qualquer uso público, configure:

```dotenv
CODE_SANDBOX_MODE=docker
CODE_SANDBOX_TIMEOUT_SECONDS=2
CODE_SANDBOX_DOCKER_IMAGE=eclipse-temurin:17-jdk
CODE_SANDBOX_DOCKER_CPUS=0.5
CODE_SANDBOX_DOCKER_MEMORY=128m
CODE_SANDBOX_DOCKER_PIDS_LIMIT=64
```

Antes de subir em produção:

```bash
docker pull eclipse-temurin:17-jdk
docker run --rm --network none eclipse-temurin:17-jdk java -version
```

Assinaturas suportadas nesta versão:

- `solve(int[] values)`
- `solve(String input)`
- `solve(int n)`
- `solve(int[] values, int target)`
- `solve(String[] values)`

Endpoints principais:

- `GET /api/code/topics/{topicId}/challenges`
- `POST /api/code/run`
- `GET /api/code/submissions?topicId=&exerciseId=`
- `GET /api/code/submissions/latest?exerciseId=`
- `GET /api/code/submissions/best?exerciseId=`

## Testes validados

- Back-end: `./mvnw.cmd test`
- Front-end contratual: `npm run test:frontend:contracts`
- Front-end completo local: `npm run test:frontend`
- E2E real: `npm run test:e2e`
- CI: backend e contratos em Linux/Windows/macOS; Playwright E2E em Linux.

## Observações para GitHub

- Não subir `.env`.
- Não subir `backend/target`, `node_modules`, perfis `.chrome-profile*`, screenshots `_verify*.png` ou zips antigos.
- O pacote final para envio deve ser gerado novamente após esta fase para incluir `package-lock.json`, `playwright.config.js`, `frontend/tests/e2e` e a migration `V7__code_submissions.sql`.
