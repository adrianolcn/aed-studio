# AED Studio - atualização avançada

Esta atualização consolida o AED Studio como uma plataforma educacional integrada para Algoritmos e Estruturas de Dados.

## Principais entregas

- Integração completa front-end/back-end com JWT, refresh token, health check público e base de API configurável.
- Catálogo educacional com trilhas, pré-requisitos, estados de tópico e progresso estruturado.
- Exercícios obrigatórios e exercícios gerados dinamicamente com persistência.
- Tentativas persistidas, feedback imediato, XP, níveis, streak e badges.
- Simuladores interativos para arrays, pilhas, filas, listas ligadas, BST, hash table e grafos.
- Missões guiadas de simulador com validação formal no back-end.
- Sistema de recomendação com score de risco, revisão por desempenho e revisão espaçada.
- Analytics educacional com acurácia, tentativas, pontos fortes, pontos de atenção, trilhas e histórico diário de XP.
- Sandbox de código Java com modo local para desenvolvimento e modo Docker recomendado para produção.
- Dashboard temático com narrativa original de exploração, mapa de progresso e próximos passos.
- Testes de back-end, contratos de front-end e teste visual E2E com Chrome headless.

## Sandbox de código

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

## Testes validados

- Back-end: `./mvnw.cmd test`
- Front-end: `npm run test:frontend`
- Verificação visual externa: Chrome headless com screenshot mobile.

## Observações para GitHub

- Não subir `.env`.
- Não subir `backend/target`, `node_modules`, perfis `.chrome-profile*`, screenshots `_verify*.png` ou zips antigos.
- O pacote final gerado para envio está em `aed-studio-github-ready-20260418.zip`.
