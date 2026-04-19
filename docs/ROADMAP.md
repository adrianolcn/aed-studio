# Roadmap

Este roadmap organiza evoluções futuras sem perder a coerência arquitetural do AED Studio.

## Próxima Fase Recomendada

### 1. Judge Multi-Linguagem

- Suporte a Python e JavaScript.
- Casos de teste por linguagem.
- Métricas separadas por linguagem.
- Sandbox Docker obrigatório em uso público.

### 2. Editor de Código Mais Rico

- Monaco Editor integrado de forma completa.
- Templates por desafio.
- Destaque de erros de compilação.
- Visualização de testes passados/falhos por cenário.

### 3. Analytics Avançado

- Curva de retenção por tópico.
- Detecção de esquecimento por intervalo de estudo.
- Recomendações com pesos ajustáveis.
- Painel de evolução por semana.

### 4. Área de Professor

- Turmas.
- Convites.
- Acompanhamento de alunos.
- Relatórios exportáveis.
- Trilhas customizadas.

### 5. Conteúdo Expandido

- Mais desafios por tópico.
- Mais geradores dinâmicos específicos.
- Simuladores com modo passo a passo avançado.
- Exercícios de prova e revisão.

## Boas Regras Para Evoluir

- O back-end continua sendo a fonte de verdade.
- Não duplicar regra pedagógica no front.
- Todo novo recurso deve ter teste proporcional.
- Todo novo endpoint deve aparecer no Swagger e na documentação.
- Recursos visuais podem rodar no front, mas eventos educacionais devem ser persistidos.
- Evitar features decorativas sem impacto real na aprendizagem.
