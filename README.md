# AED·Studio

> Plataforma educacional de Algoritmos e Estrutura de Dados com autenticação completa.
> Back-end em Spring Boot 3.2 · Java 17 · PostgreSQL · Flyway · JWT.

---

## Estado atual

- Front-end estático em `frontend/login.html` e `frontend/aed-studio.html`.
- API JWT para cadastro, login, refresh, logout e experiência educacional persistida.
- Catálogo com trilhas, pré-requisitos, progresso por tópico/trilha, exercícios, simuladores, recomendações e analytics.
- Health check público em `GET /api/health` e Swagger/OpenAPI em `/swagger-ui/index.html` e `/v3/api-docs`.
- Testes de back-end com Maven e testes de contrato do front-end com Node.

---

## Arquitetura

```
aed-studio/
├── backend/                         ← Spring Boot 3.2 + Java 17
│   ├── src/main/java/com/aedstudio/
│   │   ├── AedStudioApplication.java
│   │   ├── config/
│   │   │   ├── JwtService.java          ← geração e validação de tokens
│   │   │   ├── JwtAuthFilter.java       ← filtro JWT por requisição
│   │   │   └── SecurityConfig.java      ← API JWT + frontend estatico
│   │   ├── controller/
│   │   │   ├── AuthController.java      ← /api/auth/*
│   │   │   ├── ProgressController.java  ← /api/progress/*
│   │   │   ├── LearningController.java  ← exercícios fixos e dinâmicos
│   │   │   ├── SimulationController.java← eventos dos simuladores
│   │   │   ├── RecommendationController.java
│   │   │   └── AnalyticsController.java
│   │   ├── service/
│   │   │   ├── AuthService.java         ← register, login, refresh, logout
│   │   │   ├── ProgressService.java     ← visitas, XP, progresso, exercícios, simulações
│   │   │   ├── RecommendationService.java
│   │   │   ├── AnalyticsService.java
│   │   │   ├── TopicCatalog.java        ← lista canonica de topicos
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── model/
│   │   │   ├── User.java                ← entidade principal (implements UserDetails)
│   │   │   ├── Role.java
│   │   │   ├── RefreshToken.java        ← JWT rotation
│   │   │   ├── TopicProgress.java       ← progresso por tópico
│   │   │   ├── TopicState.java
│   │   │   ├── XpEvent.java             ← idempotência de XP
│   │   │   ├── ExerciseAttempt.java     ← tentativas persistidas
│   │   │   ├── GeneratedExercise.java   ← exercícios gerados entregues
│   │   │   ├── SimulationEvent.java     ← interação educacional nos simuladores
│   │   │   └── UserBadge.java
│   │   ├── dto/                         ← Request/Response objects
│   │   ├── repository/                  ← JPA interfaces
│   │   └── exception/                   ← handlers de erro padronizados
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   └── db/migration/                ← Flyway SQL (V1–V6)
│   └── pom.xml
├── frontend/
│   ├── login.html                       ← tela de autenticação
│   ├── api.js                           ← cliente HTTP com refresh automático
│   └── aed-studio.html                  ← plataforma principal e experiência interativa
├── .github/workflows/ci.yml             ← testes automatizados
├── .env.example                         ← template de variáveis de ambiente
├── .gitignore
├── LICENSE
└── README.md
```

---

## Pré-requisitos

Para um passo a passo completo de execução local, mobile, testes e sandbox de código, consulte [`MANUAL_EXECUCAO.md`](MANUAL_EXECUCAO.md).

| Ferramenta | Versão mínima | Download |
|---|---|---|
| Java (JDK) | 17 | https://adoptium.net |
| Maven | 3.9 | https://maven.apache.org (ou use o wrapper `mvnw` incluso) |
| PostgreSQL | 14 | https://www.postgresql.org/download |

> **Dica:** se você não quiser instalar o Maven globalmente, o projeto inclui o Maven Wrapper (`mvnw` / `mvnw.cmd`). Os comandos abaixo usam o wrapper quando possível.

---

## Passo a passo de configuração

### 1. Clonar o repositório

```bash
git clone <URL_DO_SEU_REPOSITORIO>
cd aed-studio
```

---

### 2. Criar o banco de dados no PostgreSQL

Execute o comando abaixo **uma única vez** no terminal do PostgreSQL, no pgAdmin ou em qualquer cliente SQL de sua preferência. Ele cria o banco e o usuário que a aplicação vai usar.

```sql
-- Cria o usuário da aplicação
CREATE USER aedstudio WITH PASSWORD 'troque_esta_senha';

-- Cria o banco e transfere a propriedade para o usuário
CREATE DATABASE aedstudio OWNER aedstudio;
```

> ⚠️ Use a mesma senha que você vai configurar em `DATABASE_PASSWORD` no passo seguinte.
> O Flyway criará todas as tabelas automaticamente na **primeira inicialização** — você não precisa rodar nenhum SQL adicional.

---

### 3. Configurar as variáveis de ambiente

Copie o arquivo de exemplo e preencha com seus valores:

```bash
cp .env.example .env
```

Abra o `.env` no seu editor e preencha ao menos:

```dotenv
DATABASE_URL=jdbc:postgresql://localhost:5432/aedstudio
DATABASE_USER=aedstudio
DATABASE_PASSWORD=troque_esta_senha          # mesma do passo 2

JWT_SECRET=gere_uma_chave_com_64_bytes_em_base64  # veja nota abaixo
JWT_EXPIRATION_MS=3600000                         # 1 hora
JWT_REFRESH_MS=604800000                         # 7 dias
```

**Como gerar um JWT_SECRET seguro:**

```bash
# Linux / macOS
openssl rand -base64 64

# Windows PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

---

### 4. Iniciar o back-end

Entre na pasta do back-end e execute o comando correspondente ao seu sistema operacional:

**Linux / macOS**
```bash
cd backend

# Carrega as variáveis do .env e sobe o servidor
export $(grep -v '^#' ../.env | xargs)
./mvnw spring-boot:run

# Com perfil dev explícito (logs detalhados):
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Windows — Prompt de Comando (cmd.exe)**
```cmd
cd backend

rem Carrega as variáveis do .env
for /f "tokens=1,2 delims==" %i in (..\.env) do (
    if not "%i"=="" if not "%i:~0,1%"=="#" set %i=%j
)

mvnw.cmd spring-boot:run
```

**Windows — PowerShell**
```powershell
cd backend

# Carrega as variáveis do .env
Get-Content ..\.env |
  Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } |
  ForEach-Object {
    $parts = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
  }

.\mvnw.cmd spring-boot:run
```

Aguarde a mensagem `Started AedStudioApplication` no console. O servidor estará disponível em:

```
http://localhost:8080
```

---

### 5. Verificar a inicialização

Acesse no navegador ou via curl:

```bash
# Deve retornar UP
curl -i http://localhost:8080/api/health
```

---

### 6. Abrir o front-end

Abra o arquivo `frontend/login.html` com o **Live Server** do VS Code (porta padrão 5500) ou qualquer servidor HTTP local. O CORS já está configurado para aceitar `http://localhost:5500` e `http://127.0.0.1:5500`.

---

## Rodar os testes

Os testes de integração usam H2 em memória — **não precisam de PostgreSQL rodando**.

**Linux / macOS**
```bash
cd backend
./mvnw test
```

**Windows**
```cmd
cd backend
mvnw.cmd test
```

**Front-end**
```bash
npm run test:frontend
```

---

## API Reference

### Autenticação — `/api/auth`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/register` | ✗ | Cadastro + retorna tokens JWT |
| `POST` | `/login` | ✗ | Login JWT (API / SPA) |
| `POST` | `/refresh` | ✗ | Renova access token via refresh token |
| `POST` | `/logout` | ✓ JWT | Revoga refresh token |
| `GET` | `/me` | ✓ | Dados do usuário autenticado |

### Progresso — `/api/progress`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/` | ✓ | Estado completo de progresso |
| `POST` | `/visit` | ✓ | Registra visita a tópico |
| `POST` | `/xp` | ✓ | Concede XP (idempotente) |

### Catálogo — `/api/catalog`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/topics` | ✗ | Lista trilhas, tópicos, pré-requisitos e total oficial |

### Aprendizagem — `/api/learning`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/topics/{topicId}/exercises` | ✓ | Exercícios obrigatórios do tópico |
| `POST` | `/attempts` | ✓ | Corrige exercício obrigatório e atualiza progresso |
| `POST` | `/generated-exercises` | ✓ | Gera e persiste nova variação de exercício |
| `GET` | `/generated-exercises/history` | ✓ | Histórico dos exercícios gerados entregues ao aluno |
| `POST` | `/generated-exercises/{id}/attempts` | ✓ | Corrige exercício gerado usando a versão persistida |

### Código — `/api/code`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/topics/{topicId}/challenges` | ✓ | Lista desafios de código seguros do tópico |
| `POST` | `/run` | ✓ | Compila e executa o corpo da solução em sandbox com timeout e política restritiva |

### Simuladores — `/api/simulations`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/events` | ✓ | Registra interação relevante do simulador e concede XP idempotente quando aplicável |
| `GET` | `/topics/{topicId}/missions` | ✓ | Lista missões guiadas e critérios formais do simulador |
| `POST` | `/missions/{missionId}/submit` | ✓ | Valida estado do simulador e concede XP idempotente pela missão |

### Recomendações — `/api/recommendations`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/` | ✓ | Retorna recomendação principal, próximos passos, revisões e foco de trilha |

### Analytics — `/api/analytics`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/overview` | ✓ | Visão geral de acerto, tentativas, simulações, XP e pontos de atenção |
| `GET` | `/topics` | ✓ | Métricas por tópico, com insight derivado dos dados |
| `GET` | `/trails` | ✓ | Métricas por trilha |
| `GET` | `/xp-history` | ✓ | Série temporal diária de XP e acumulado |

### Exemplos

**Login JWT:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "aluno@email.com",
  "password": "Senha123"
}
```

**Resposta:**
```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "aluno",
    "email": "aluno@email.com",
    "fullName": "Nome Completo",
    "role": "STUDENT",
    "xp": 0,
    "streakDays": 1
  }
}
```

**Conceder XP:**
```http
POST /api/progress/xp
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "topicId": "arrays",
  "reason": "quiz_arrays-q1",
  "amount": 10
}
```

---

## Fluxo de autenticação

```
┌─────────────────────────────────────────────────────────────────┐
│  JWT (para API e SPA estatica)                                  │
│                                                                  │
│  1. POST /api/auth/login → { accessToken, refreshToken }        │
│  2. Cada request: Authorization: Bearer <accessToken>           │
│  3. accessToken expira → POST /api/auth/refresh → novo par      │
│  4. Logout: POST /api/auth/logout (revoga refreshToken no DB)   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Integração com o front-end

Inclua `api.js` antes do script principal do `aed-studio.html`:

```html
<script src="api.js"></script>
```

No início do script principal:

```javascript
// Verifica autenticação — redireciona para login se necessário
const user = await AedApi.requireAuth();

// Ao navegar para um tópico:
AedApi.recordVisit('arrays');

// Ao acertar um quiz:
AedApi.awardXp('arrays', 'quiz_arrays-q1', 10);

// Sincronizar progresso ao carregar:
const progress = await AedApi.getProgress();
// progress.topics     → { "arrays": "VISITED", "graphs": "COMPLETED", ... }
// progress.totalXp    → total de XP acumulado
// progress.streakDays → sequência de dias estudados

// Recomendação e analytics:
const recommendations = await AedApi.getRecommendations();
const analytics = await AedApi.getAnalyticsOverview();

// Simuladores e exercícios dinâmicos:
await AedApi.recordSimulationEvent('arrays', 'ARRAY', 'insert', 'FIRST_RUN', '[3]');
const missions = await AedApi.getSimulatorMissions('arrays');
await AedApi.submitSimulatorMission(missions[0].id, '{"values":[3,6,9],"edges":[]}');
const generated = await AedApi.generateExercise('arrays', 1);
await AedApi.submitGeneratedExercise(generated.id, 'A', 18);

const challenges = await AedApi.getCodeChallenges('arrays');
await AedApi.runCode(challenges[0].id, 'int total = 0; for (int value : values) total += value; return total;');
```

---

## Sandbox de Código em Produção

O AED·Studio possui dois modos de execução para desafios de código:

| Modo | Uso recomendado | Como funciona |
|---|---|---|
| `local` | Desenvolvimento e testes automatizados | Compila e executa em processo local com timeout e bloqueio de APIs perigosas |
| `docker` | Produção e qualquer uso público | Executa cada submissão em container efêmero, sem rede, com filesystem de entrada somente leitura, `tmpfs`, limite de CPU, memória e PIDs |

Para ativar o modo recomendado em produção:

```dotenv
CODE_SANDBOX_MODE=docker
CODE_SANDBOX_TIMEOUT_SECONDS=2
CODE_SANDBOX_DOCKER_IMAGE=eclipse-temurin:17-jdk
CODE_SANDBOX_DOCKER_CPUS=0.5
CODE_SANDBOX_DOCKER_MEMORY=128m
CODE_SANDBOX_DOCKER_PIDS_LIMIT=64
```

Pré-requisitos:

```bash
docker pull eclipse-temurin:17-jdk
docker run --rm --network none eclipse-temurin:17-jdk java -version
```

O comando gerado pelo back-end usa:

- `--network none`
- `--read-only`
- `--tmpfs /tmp:rw,noexec,nosuid,size=64m`
- `--security-opt no-new-privileges`
- `--cpus`, `--memory` e `--pids-limit`
- montagem do workspace como `/workspace:ro`

Isso reduz bastante o risco de execução de código de aluno em ambiente compartilhado. Para escala pública maior, o próximo passo natural é mover o executor para workers dedicados, com fila, observabilidade e política de limpeza/agendamento.

---

## Decisões de segurança

| Decisão | Justificativa |
|---|---|
| **BCrypt custo 12** | ~300ms por hash — equilibra segurança e performance |
| **Refresh token rotation** | Cada renovação invalida o token anterior, impedindo roubo silencioso |
| **XP idempotente** | `UNIQUE(user_id, event_key)` no banco impede double-award mesmo com retry |
| **JWT_SECRET obrigatório em produção** | O perfil `prod` falha ao iniciar sem segredo explícito |
| **Access token de curta duração** | Padrão de 1 hora, configurável por `JWT_EXPIRATION_MS` |
| **JWT sem dados sensíveis** | Payload contém apenas email e role — nunca senha ou PII |
| **CORS restrito** | Apenas origens explícitas em `cors.allowed-origins` são aceitas |
| **`.env` fora do repositório** | Credenciais nunca chegam ao git (ver seção abaixo) |

---

## 🔐 Boas práticas — Cofre de senhas

> **Leia antes de fazer seu primeiro commit.**

O arquivo `.env` está no `.gitignore` e **nunca será versionado** — isso é correto e intencional. O risco é perder suas variáveis de desenvolvimento ao trocar de máquina ou formatar o sistema.

**Procedimento recomendado:**

1. Após preencher o `.env` com seus valores reais, abra o **Bitwarden** (ou qualquer gerenciador de senhas de sua confiança).
2. Crie uma nota segura chamada, por exemplo, `AED·Studio — .env local`.
3. Cole o conteúdo completo do `.env` nessa nota e salve.
4. Faça isso sempre que alterar qualquer variável de ambiente do projeto.

Isso garante que você poderá recriar o ambiente de desenvolvimento em qualquer máquina em menos de dois minutos, sem depender de backup de arquivos.

---

## Licença

Copyright © 2026 Adriano Lucas Conceição Nunes. Todos os direitos reservados.
Consulte o arquivo [`LICENSE`](LICENSE) para os termos completos.
