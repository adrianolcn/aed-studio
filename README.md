# AED·Studio

> Plataforma educacional de Algoritmos e Estrutura de Dados com autenticação completa.
> Back-end em Spring Boot 3.2 · Java 17 · PostgreSQL · Flyway · JWT + Sessão.

---

## Screenshots

| Tela de Login | Plataforma |
|---|---|
| ![Login](docs/login.png) | ![Plataforma](docs/platform.png) |

| Progresso do aluno | Simulador de estruturas |
|---|---|
| ![Progresso](docs/progress.png) | ![Simulador](docs/simulator.png) |

> **Para adicionar as imagens:** coloque os arquivos `.png` na pasta `docs/` e os badges acima serão renderizados automaticamente no GitHub.

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
│   │   │   └── SecurityConfig.java      ← duas cadeias: JWT + sessão
│   │   ├── controller/
│   │   │   ├── AuthController.java      ← /api/auth/*
│   │   │   └── ProgressController.java  ← /api/progress/*
│   │   ├── service/
│   │   │   ├── AuthService.java         ← register, login, refresh, logout
│   │   │   ├── ProgressService.java     ← visitas, XP, progresso
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── model/
│   │   │   ├── User.java                ← entidade principal (implements UserDetails)
│   │   │   ├── Role.java
│   │   │   ├── RefreshToken.java        ← JWT rotation
│   │   │   ├── TopicProgress.java       ← progresso por tópico
│   │   │   ├── TopicState.java
│   │   │   └── XpEvent.java             ← idempotência de XP
│   │   ├── dto/                         ← Request/Response objects
│   │   ├── repository/                  ← JPA interfaces
│   │   └── exception/                   ← handlers de erro padronizados
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   └── db/migration/                ← Flyway SQL (V1–V4)
│   └── pom.xml
├── frontend/
│   ├── login.html                       ← tela de autenticação
│   ├── api.js                           ← cliente HTTP com refresh automático
│   └── aed-studio.html                  ← plataforma principal
├── docs/                                ← screenshots (não versionados os binários)
├── .env.example                         ← template de variáveis de ambiente
├── .gitignore
├── LICENSE
└── README.md
```

---

## Pré-requisitos

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

JWT_SECRET=gere_uma_chave_com_64_caracteres  # veja nota abaixo
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
# Deve retornar 401 (servidor rodando, rota protegida)
curl -i http://localhost:8080/api/auth/me
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

---

## API Reference

### Autenticação — `/api/auth`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/register` | ✗ | Cadastro + retorna tokens JWT |
| `POST` | `/login` | ✗ | Login JWT (API / SPA) |
| `POST` | `/login-web` | ✗ | Login sessão (front-end web) |
| `POST` | `/refresh` | ✗ | Renova access token via refresh token |
| `POST` | `/logout` | ✓ JWT | Revoga refresh token |
| `POST` | `/logout-web` | ✓ Sessão | Invalida sessão HTTP |
| `GET` | `/me` | ✓ | Dados do usuário autenticado |

### Progresso — `/api/progress`

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/` | ✓ | Estado completo de progresso |
| `POST` | `/visit` | ✓ | Registra visita a tópico |
| `POST` | `/xp` | ✓ | Concede XP (idempotente) |

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
  "expiresIn": 86400,
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
  "topicId": "tad",
  "reason": "quiz_tad-q1",
  "amount": 10
}
```

---

## Fluxo de autenticação dual

```
┌─────────────────────────────────────────────────────────────────┐
│  MODO 1 — JWT (para API e SPA)                                  │
│                                                                  │
│  1. POST /api/auth/login → { accessToken, refreshToken }        │
│  2. Cada request: Authorization: Bearer <accessToken>           │
│  3. accessToken expira → POST /api/auth/refresh → novo par      │
│  4. Logout: POST /api/auth/logout (revoga refreshToken no DB)   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  MODO 2 — Sessão HTTP (para front-end web)                      │
│                                                                  │
│  1. POST /api/auth/login-web → cookie JSESSIONID                │
│  2. Navegador envia o cookie automaticamente em cada request    │
│  3. Sessão persistida no PostgreSQL (spring_session tables)     │
│  4. Logout: POST /api/auth/logout-web (invalida sessão)         │
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
AedApi.awardXp('tad', 'quiz_tad-q1', 10);

// Sincronizar progresso ao carregar:
const progress = await AedApi.getProgress();
// progress.topics     → { "arrays": "VISITED", "tad": "COMPLETED", ... }
// progress.totalXp    → total de XP acumulado
// progress.streakDays → sequência de dias estudados
```

---

## Decisões de segurança

| Decisão | Justificativa |
|---|---|
| **BCrypt custo 12** | ~300ms por hash — equilibra segurança e performance |
| **Refresh token rotation** | Cada renovação invalida o token anterior, impedindo roubo silencioso |
| **XP idempotente** | `UNIQUE(user_id, event_key)` no banco impede double-award mesmo com retry |
| **Sessões no PostgreSQL** | Sobrevive a restarts do servidor; não depende de memória |
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
