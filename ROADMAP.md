# Roadmap do AED Studio

Este arquivo resume o caminho de evolução visível na raiz do repositório. A versão detalhada fica em [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Estado Atual

- Plataforma full-stack com front-end estático e API Spring Boot.
- Autenticação JWT com refresh token.
- Catálogo educacional, trilhas, pré-requisitos, progresso, XP, streak e badges.
- Exercícios fixos/dinâmicos, simuladores, recomendações, analytics e judge Java.
- Testes de contrato do front, testes de back-end e Playwright E2E.
- Execução local tradicional e base para Docker Compose.

## Próximas Fases

1. Completar extração gradual do front-end para módulos por domínio.
2. Evoluir o judge para workers isolados e sandbox Docker obrigatório em ambiente público.
3. Ampliar E2E visual com fluxos por simulador e exercício de código.
4. Criar área de professor, turmas e relatórios.
5. Expandir conteúdo, desafios e geradores dinâmicos por tópico.

## Princípios

- Back-end é a fonte de verdade pedagógica.
- Front-end pode animar e visualizar, mas não inventar progresso.
- Toda nova regra deve ter teste proporcional.
- Toda mudança operacional deve atualizar documentação.
