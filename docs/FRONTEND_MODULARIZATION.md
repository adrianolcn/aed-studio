# Modularização do Front-end

O front-end do AED Studio continua sendo servido como HTML estático para preservar simplicidade operacional e facilitar demonstração em portfólio. A modularização atual é incremental: módulos ES vivem em `frontend/js/` e convivem com os scripts globais existentes.

## Objetivo

- reduzir acoplamento em novas evoluções;
- criar fronteiras por domínio sem migrar para React/Vue;
- permitir testes e extrações futuras com menor risco;
- preservar `frontend/login.html`, `frontend/aed-studio.html` e `frontend/api.js`.

## Estrutura

```text
frontend/js/
├── api/client.js                  # adaptador do AedApi global
├── auth/session.js                # sessão/autenticação
├── learning/catalog.js            # estados e fluxo pedagógico
├── progress/progress-view.js      # formatação de progresso/XP
├── simulators/registry.js         # tipos de simulador suportados
├── analytics/analytics-view.js    # leitura visual de métricas
├── recommendations/recommendation-view.js
├── sandbox/code-judge.js
├── shared/dom.js
└── index.js                       # registry exposto em window.AedStudioModules
```

## Regra de evolução

Código novo deve preferir importar módulos de `frontend/js/`. Código legado pode continuar usando funções globais enquanto a extração gradual acontece.

## Próximas extrações recomendadas

1. mover renderização de dashboard para `frontend/js/dashboard/`;
2. mover simuladores para módulos por estrutura;
3. mover analisador assintótico e Sorting Arena para módulos próprios;
4. reduzir o script inline de `aed-studio.html` até ele virar apenas inicialização.
