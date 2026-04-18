# Manual de Execução do AED Studio

Este manual mostra como colocar o AED Studio para rodar de verdade em ambiente local, com back-end, front-end, banco de dados, autenticação, progresso, simuladores, exercícios, analytics e sandbox de código.

## 1. O Que Você Vai Rodar

O projeto tem duas partes principais:

- Back-end Spring Boot: API, autenticação, progresso, exercícios, simuladores, analytics, recomendações e sandbox.
- Front-end estático: telas `login.html` e `aed-studio.html`, consumindo a API pelo `api.js`.

Fluxo esperado:

```text
Navegador -> frontend/login.html -> API Spring Boot -> PostgreSQL
```

Para desafios de código em produção:

```text
API Spring Boot -> Docker -> container isolado -> resultado do desafio
```

## 2. Pré-Requisitos

Instale:

- Java JDK 17 ou superior.
- PostgreSQL 14 ou superior.
- Node.js 20 ou superior, para testes do front-end.
- Docker Desktop, recomendado para sandbox de código em modo produção.
- VS Code com extensão Live Server, ou qualquer servidor HTTP simples para abrir o front-end.

Verifique no terminal:

```bash
java -version
node -v
docker --version
```

No Windows, use PowerShell de preferência.

## 3. Preparar o Banco PostgreSQL

Abra o PostgreSQL, pgAdmin ou terminal `psql` e execute:

```sql
CREATE USER aedstudio WITH PASSWORD 'troque_esta_senha';
CREATE DATABASE aedstudio OWNER aedstudio;
```

O Flyway cria as tabelas automaticamente quando o back-end iniciar.

## 4. Criar o Arquivo `.env`

Na raiz do projeto, copie:

```bash
copy .env.example .env
```

No PowerShell, também pode usar:

```powershell
Copy-Item .env.example .env
```

Abra o `.env` e configure pelo menos:

```dotenv
DATABASE_URL=jdbc:postgresql://localhost:5432/aedstudio
DATABASE_USER=aedstudio
DATABASE_PASSWORD=troque_esta_senha

JWT_SECRET=TROQUE_POR_UMA_CHAVE_SECRETA_LONGA_E_ALEATORIA_DE_AO_MENOS_256_BITS
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_MS=604800000

PORT=8080
CORS_ORIGINS=http://localhost:5500,http://127.0.0.1:5500
CORS_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*,http://192.168.*.*:*,http://10.*.*.*:*

SPRING_PROFILES_ACTIVE=dev
CODE_SANDBOX_MODE=local
```

Para gerar um `JWT_SECRET` no PowerShell:

```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

Importante: não suba o `.env` para o GitHub.

## 5. Carregar o `.env` no PowerShell

Antes de iniciar o back-end, rode este comando na raiz do projeto:

```powershell
Get-Content .env |
  Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } |
  ForEach-Object {
    $parts = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
  }
```

Isso carrega as variáveis apenas para o terminal atual.

## 6. Iniciar o Back-End

Entre na pasta `backend`:

```powershell
cd backend
```

Inicie:

```powershell
.\mvnw.cmd spring-boot:run
```

Aguarde aparecer algo parecido com:

```text
Started AedStudioApplication
```

A API estará em:

```text
http://localhost:8080
```

Teste o health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Resposta esperada:

```json
{
  "status": "UP"
}
```

## 7. Abrir o Front-End

Com o back-end rodando, abra o front-end por um servidor local. Esse ponto é importante: abrir `frontend/aed-studio.html` com duplo clique, em modo `file://`, serve apenas para inspeção visual do HTML. A experiência real com login, progresso salvo, XP, simuladores integrados, recomendações e analytics precisa passar pelo servidor local e pela API Spring Boot.

Opção recomendada no VS Code:

1. Abra a pasta do projeto no VS Code.
2. Clique com o botão direito em `frontend/login.html`.
3. Escolha `Open with Live Server`.
4. A URL deve ficar parecida com:

```text
http://localhost:5500/frontend/login.html
```

Crie uma conta pela aba `CRIAR CONTA`.

Depois do cadastro ou login, a aplicação deve abrir:

```text
frontend/aed-studio.html
```

Se a tela principal for aberta diretamente pelo arquivo estático, ela não deve ficar escurecida nem travada, mas também não vai persistir progresso porque não existe sessão autenticada nesse modo.

## 8. Rodar em Celular na Mesma Rede

Esse passo serve para testar mobile real.

1. Descubra o IP da sua máquina:

```powershell
ipconfig
```

Procure o IPv4 da sua rede Wi-Fi, por exemplo:

```text
192.168.0.25
```

2. Inicie o back-end normalmente em `localhost:8080`.

3. Abra o front pelo Live Server usando o IP da máquina:

```text
http://192.168.0.25:5500/frontend/login.html
```

4. Se necessário, configure a base da API pela URL:

```text
http://192.168.0.25:5500/frontend/login.html?apiBase=http://192.168.0.25:8080
```

5. Garanta que o firewall do Windows permita acesso às portas:

- `5500`, front-end.
- `8080`, back-end.

## 9. Ativar Sandbox Docker Para Produção

Para desenvolvimento, o projeto usa:

```dotenv
CODE_SANDBOX_MODE=local
```

Para produção ou uso público, altere para:

```dotenv
CODE_SANDBOX_MODE=docker
CODE_SANDBOX_TIMEOUT_SECONDS=2
CODE_SANDBOX_DOCKER_IMAGE=eclipse-temurin:17-jdk
CODE_SANDBOX_DOCKER_CPUS=0.5
CODE_SANDBOX_DOCKER_MEMORY=128m
CODE_SANDBOX_DOCKER_PIDS_LIMIT=64
```

Antes, baixe a imagem:

```powershell
docker pull eclipse-temurin:17-jdk
```

Teste:

```powershell
docker run --rm --network none eclipse-temurin:17-jdk java -version
```

O modo Docker roda cada tentativa de código em container isolado, sem rede e com limites de recurso.

## 10. Rodar Testes

Back-end:

```powershell
cd backend
.\mvnw.cmd test
```

Resultado esperado:

```text
BUILD SUCCESS
```

Front-end:

Na raiz do projeto:

```powershell
npm run test:frontend
```

Observação: o teste visual com Chrome pode ser pulado se o ambiente bloquear execução headless pelo Node. Isso é esperado em alguns sandboxes. Para validar manualmente, rode Chrome headless fora do sandbox ou teste pelo navegador.

## 11. URLs Úteis

API:

```text
http://localhost:8080
```

Health:

```text
http://localhost:8080/api/health
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Login:

```text
http://localhost:5500/frontend/login.html
```

Plataforma:

```text
http://localhost:5500/frontend/aed-studio.html
```

## 12. Fluxo Para Validar Que Tudo Está Funcionando

Faça este roteiro:

1. Abra `login.html`.
2. Crie uma conta.
3. Entre na plataforma.
4. Veja o dashboard carregar XP, progresso, nível e streak.
5. Abra o primeiro tópico `Algoritmo`.
6. Responda o exercício obrigatório.
7. Gere um exercício dinâmico.
8. Interaja com um simulador em tópico disponível.
9. Execute uma missão do simulador.
10. Rode um desafio no sandbox de código.
11. Volte ao dashboard e confira analytics, XP e recomendações.

Se tudo isso funcionar, a integração está ativa de ponta a ponta.

## 13. Problemas Comuns

### A tela diz que a API está offline

Verifique:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Se falhar, o back-end não está rodando ou a porta está errada.

### Login funciona, mas a plataforma redireciona para login

Possíveis causas:

- Token expirado.
- `JWT_SECRET` mudou entre execuções.
- `sessionStorage` antigo.

Solução:

1. Saia da conta.
2. Limpe dados do site no navegador.
3. Faça login novamente.

### Erro de CORS no navegador

Confirme se a origem do front está em:

```dotenv
CORS_ORIGINS=
CORS_ORIGIN_PATTERNS=
```

Para rede local/mobile, mantenha:

```dotenv
CORS_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*,http://192.168.*.*:*,http://10.*.*.*:*
```

### PostgreSQL não conecta

Confira:

- banco `aedstudio` existe;
- usuário `aedstudio` existe;
- senha no `.env` é a mesma do banco;
- PostgreSQL está rodando;
- porta padrão `5432` está disponível.

### Sandbox Docker falha

Verifique:

```powershell
docker --version
docker pull eclipse-temurin:17-jdk
docker run --rm --network none eclipse-temurin:17-jdk java -version
```

Se Docker não estiver disponível, use temporariamente:

```dotenv
CODE_SANDBOX_MODE=local
```

### GitHub Actions falha em poucos segundos no job backend

Quando o projeto é preparado no Windows, o arquivo `backend/mvnw` pode chegar ao GitHub sem permissão de execução. O workflow atual cobre isso e valida a aplicação nos três sistemas principais:

- Linux: usa `backend/mvnw` após `chmod +x`.
- macOS: usa `backend/mvnw` após `chmod +x`.
- Windows: usa `backend/mvnw.cmd`.

O CI também roda os testes de front-end em Linux, macOS e Windows. Se aparecer um erro antigo como `Permission denied` em `./mvnw test`, atualize o repositório com a versão mais recente de `.github/workflows/ci.yml`.

Os testes de contrato do front-end são obrigatórios nos três sistemas. O smoke visual com navegador é separado porque depende do Chrome headless disponível no runner; no GitHub Actions ele roda apenas no Linux. Em ambiente local, use:

```powershell
npm run test:frontend:contracts
npm run test:frontend:visual
```

## 14. Checklist Antes de Subir Para o GitHub

Não suba:

- `.env`
- `backend/target`
- `node_modules`
- `.chrome-profile*`
- `_verify*.png`
- arquivos `.zip` antigos

Suba:

- `backend`
- `frontend`
- `README.md`
- `MANUAL_EXECUCAO.md`
- `GITHUB_RELEASE_NOTES.md`
- `.env.example`
- `package.json`
- `.github`, se quiser manter CI

## 15. Comandos Rápidos

Back-end:

```powershell
Get-Content .env |
  Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } |
  ForEach-Object {
    $parts = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
  }

cd backend
.\mvnw.cmd spring-boot:run
```

Testes:

```powershell
cd backend
.\mvnw.cmd test
cd ..
npm run test:frontend
```

Produção com Docker sandbox:

```powershell
docker pull eclipse-temurin:17-jdk
$env:CODE_SANDBOX_MODE="docker"
cd backend
.\mvnw.cmd spring-boot:run
```
