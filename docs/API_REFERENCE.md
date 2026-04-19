# API Reference

Base local padrão:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

## Health

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/health` | Não | Verifica disponibilidade pública da API |

## Auth

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/register` | Não | Cadastra usuário e retorna tokens |
| `POST` | `/api/auth/login` | Não | Autentica usuário |
| `POST` | `/api/auth/refresh` | Não | Renova access token |
| `POST` | `/api/auth/logout` | Sim | Revoga refresh token |
| `GET` | `/api/auth/me` | Sim | Retorna usuário autenticado |

## Catálogo

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/catalog/topics` | Não | Retorna trilhas, tópicos, pré-requisitos e total oficial |

## Progresso

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/progress` | Sim | Estado consolidado de progresso |
| `POST` | `/api/progress/visit` | Sim | Registra visita a tópico |
| `POST` | `/api/progress/xp` | Sim | Concede XP idempotente |

## Aprendizagem

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/learning/topics/{topicId}/exercises` | Sim | Lista exercícios obrigatórios do tópico |
| `POST` | `/api/learning/attempts` | Sim | Corrige exercício obrigatório |
| `POST` | `/api/learning/generated-exercises` | Sim | Gera e persiste exercício dinâmico |
| `GET` | `/api/learning/generated-exercises/history` | Sim | Histórico dos exercícios gerados |
| `POST` | `/api/learning/generated-exercises/{id}/attempts` | Sim | Corrige exercício gerado persistido |

## Simuladores

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/simulations/events` | Sim | Registra evento educacional do simulador |
| `GET` | `/api/simulations/topics/{topicId}/missions` | Sim | Lista missões do simulador |
| `POST` | `/api/simulations/missions/{missionId}/submit` | Sim | Valida missão e concede XP idempotente |

## Code Judge

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/code/topics/{topicId}/challenges` | Sim | Lista desafios de código do tópico |
| `POST` | `/api/code/run` | Sim | Executa solução no judge e persiste submissão |
| `GET` | `/api/code/submissions?topicId=&exerciseId=` | Sim | Lista histórico filtrável de submissões |
| `GET` | `/api/code/submissions/latest?exerciseId=` | Sim | Última submissão do desafio |
| `GET` | `/api/code/submissions/best?exerciseId=` | Sim | Melhor submissão do desafio |

Assinaturas suportadas:

- `solve(int[] values)`
- `solve(String input)`
- `solve(int n)`
- `solve(int[] values, int target)`
- `solve(String[] values)`

Exemplo de submissão:

```http
POST /api/code/run
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "challengeId": "arrays-code-search",
  "code": "for (int i = 0; i < values.length; i++) { if (values[i] == target) return i; } return -1;"
}
```

Resposta resumida:

```json
{
  "accepted": true,
  "status": "SUCCESS",
  "submissionId": 42,
  "passedCount": 3,
  "totalChecks": 3,
  "executionTimeMs": 211,
  "feedback": "Todos os 3 cenários passaram."
}
```

## Recomendações

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/recommendations` | Sim | Próximo passo, revisões e justificativas |

## Analytics

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/analytics/overview` | Sim | Visão geral do aluno |
| `GET` | `/api/analytics/topics` | Sim | Métricas por tópico |
| `GET` | `/api/analytics/trails` | Sim | Métricas por trilha |
| `GET` | `/api/analytics/xp-history` | Sim | Série temporal de XP |
