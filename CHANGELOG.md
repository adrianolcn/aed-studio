# Changelog

## 2.2.0 - Maturidade de Repositório e Operação

### Adicionado

- Templates de issue e pull request em `.github/`.
- Workflow de CI com jobs de back-end, front-end e Playwright.
- `docker-compose.yml` para subir banco, API e front-end.
- Dockerfiles para back-end e front-end.
- `backend/pom.xml` com dependências Spring Boot/Java 17.
- Camada modular incremental em `frontend/js/`.
- Documentação de operação, sandbox e modularização do front-end.
- `ROADMAP.md` na raiz para leitura rápida no GitHub.

### Alterado

- README e manual passam a documentar caminho rápido com Docker Compose.
- Documentação passa a usar Maven direto (`mvn`) como comando padrão.
- Testes contratuais validam a presença da camada modular do front-end.

## 2.1.0 - Code Judge, Submissões e E2E

### Adicionado

- Judge Java com múltiplas assinaturas controladas.
- Persistência de submissões de código.
- Histórico de submissões por desafio.
- Métricas de código em analytics.
- Recomendações considerando desempenho em código.
- Playwright E2E real com backend e frontend.
- Perfil `e2e` com H2 em memória.
- Documentação de arquitetura, API, testes, CI, segurança e roadmap.

### Alterado

- Painéis de aprendizagem no front agora usam inserção segura no DOM.
- Renderização assíncrona de painéis evita duplicação.
- Feedback do sandbox permanece visível após atualização de progresso.
- CI valida Linux, Windows e macOS para backend/front e E2E no Linux.

### Corrigido

- Painéis de simulador/código não apareciam em páginas com conteúdo interno.
- Resultado do judge era apagado por rerender imediato.
- E2E navegava para um ID de página inexistente.

## 2.0.0 - Plataforma Educacional Integrada

### Adicionado

- Trilhas educacionais.
- Pré-requisitos.
- Progresso persistido.
- Exercícios fixos e dinâmicos.
- Simuladores interativos.
- XP, níveis, streak e badges.
- Recomendações.
- Analytics educacional.
- Swagger/OpenAPI.

## 1.0.0 - Base JWT e Integração

### Adicionado

- Cadastro, login, refresh token e logout.
- Cliente HTTP central no front.
- Health check público.
- Progresso inicial persistido.
