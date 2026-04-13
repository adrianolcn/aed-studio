# 🚀 Guia Completo: Do Zero ao GitHub Privado
### AED·Studio — Passo a passo para iniciantes no terminal

> **Para quem é este guia?**
> Para quem entende o projeto que construiu, mas ainda não se sente
> à vontade com Git e terminal. Cada passo aqui vai te explicar não só
> *o que fazer*, mas *por que* você está fazendo.

---

## Antes de começar — entendendo o que é Git em 30 segundos

Imagine que o Git é uma **máquina do tempo para código**. A cada
"commit" que você faz, você tira uma foto do projeto naquele momento.
Se algo quebrar no futuro, você pode voltar para qualquer foto anterior.

O GitHub é onde essas fotos ficam guardadas na nuvem — de forma privada,
neste caso, então só você terá acesso.

---

## 📁 ETAPA 1 — Como a pasta do projeto deve estar organizada

Abra o seu explorador de arquivos (Finder no Mac, File Explorer no
Windows) e navegue até a pasta `aed-studio`. Ela deve estar assim:

```
📁 aed-studio/                  ← esta é a RAIZ do projeto
│
├── 📄 .env.example              ← modelo das variáveis (vai pro GitHub)
├── 📄 .env                      ← suas senhas REAIS (NÃO vai pro GitHub)
├── 📄 .gitignore                ← lista do que o Git deve ignorar
├── 📄 LICENSE                   ← licença proprietária
├── 📄 README.md                 ← documentação do projeto
│
├── 📁 backend/                  ← todo o código Java fica aqui
│   ├── 📄 mvnw                  ← botão de iniciar (Linux/Mac)
│   ├── 📄 mvnw.cmd              ← botão de iniciar (Windows)
│   ├── 📄 pom.xml               ← lista de dependências Java
│   ├── 📁 .mvn/wrapper/         ← configuração do Maven Wrapper
│   └── 📁 src/                  ← código-fonte Java
│
├── 📁 frontend/                 ← código HTML/JS fica aqui
│   ├── 📄 login.html
│   └── 📄 api.js
│
└── 📁 docs/                     ← pasta para os screenshots futuros
```

> ✅ **Checklist rápido:**
> - [ ] O arquivo `.env` existe e está fora do `backend/`? (na raiz)
> - [ ] O arquivo `.env.example` existe?
> - [ ] Existe um `README.md` na raiz?
> - [ ] Existe um `LICENSE` na raiz?

---

## 🔧 ETAPA 2 — Instalações necessárias (apenas uma vez)

### 2.1 — Instalar o Git

O Git é o programa que faz o versionamento. Sem ele, nada funciona.

- **Windows:** Baixe em https://git-scm.com/download/win
  Durante a instalação, aceite todas as opções padrão.
  Quando terminar, abra o menu Iniciar e procure por "Git Bash" —
  é este terminal que você vai usar no Windows.

- **macOS:** Abra o Terminal e rode:
  ```
  git --version
  ```
  Se aparecer um número de versão, já está instalado. Se não, o macOS
  vai te perguntar se quer instalar as ferramentas de linha de comando
  — clique em "Instalar".

- **Linux (Ubuntu/Debian):**
  ```bash
  sudo apt install git -y
  ```

### 2.2 — Instalar o VS Code (se ainda não tiver)

Baixe em: https://code.visualstudio.com

O VS Code tem uma interface visual para Git que vai evitar que você
precise memorizar comandos.

### 2.3 — Instalar a extensão "Conventional Commits" no VS Code

Esta extensão vai te guiar para escrever mensagens de commit
no formato profissional, com um formulário visual.

1. Abra o VS Code
2. Pressione `Ctrl+Shift+X` (Windows/Linux) ou `Cmd+Shift+X` (Mac)
   para abrir a aba de extensões
3. No campo de busca, digite: `conventional commits`
4. Clique em **Instalar** na extensão de **vivaxy**
   (ícone de check verde, com mais de 1 milhão de downloads)

---

## 🌐 ETAPA 3 — Criar o repositório privado no GitHub

### 3.1 — Criar uma conta (se ainda não tiver)

Acesse https://github.com e crie uma conta gratuita.

### 3.2 — Criar o repositório

1. Após fazer login, clique no botão verde **"New"** no canto
   superior esquerdo (ou acesse https://github.com/new)

2. Preencha o formulário:
   - **Repository name:** `aed-studio`
   - **Description:** `Plataforma educacional de AED com autenticação Spring Boot + JWT`
   - **Visibility:** selecione `🔒 Private` ← **muito importante!**
   - ⚠️ **NÃO marque** nenhuma das caixas "Initialize this repository"
     (README, .gitignore, LICENSE). Você já tem esses arquivos.

3. Clique em **"Create repository"**

4. O GitHub vai mostrar uma tela com instruções. Deixe essa aba
   aberta — você vai precisar do endereço do repositório em breve.
   Ele terá um formato parecido com:
   ```
   https://github.com/seu-usuario/aed-studio.git
   ```

---

## 🔑 ETAPA 4 — Conectar o VS Code ao GitHub com segurança

O GitHub não aceita mais login com usuário e senha pelo terminal.
Você precisa de um **Token de Acesso Pessoal** — pense nele como
uma senha especial gerada só para o terminal.

### 4.1 — Gerar o Personal Access Token (PAT)

1. No GitHub, clique na sua foto de perfil (canto superior direito)
2. Clique em **Settings**
3. Role até o final da barra lateral esquerda e clique em
   **"Developer settings"**
4. Clique em **"Personal access tokens"** → **"Tokens (classic)"**
5. Clique em **"Generate new token"** → **"Generate new token (classic)"**
6. Preencha:
   - **Note:** `aed-studio local`
   - **Expiration:** `90 days` (ou mais, se preferir)
   - **Marque a caixa:** `repo` (isso dá acesso a repositórios privados)
7. Clique em **"Generate token"** no final da página
8. O token vai aparecer **uma única vez** — uma sequência começando
   com `ghp_...`. **Copie agora** e salve no Bitwarden como
   `GitHub PAT — aed-studio`. Você nunca mais vai conseguir ver
   este token no GitHub.

---

## 💻 ETAPA 5 — Abrir a pasta no VS Code

1. Abra o VS Code
2. Clique em **File → Open Folder** (ou `Ctrl+K Ctrl+O`)
3. Navegue até a pasta `aed-studio` (a raiz, não o `backend`)
4. Clique em **"Selecionar Pasta"**

O VS Code vai abrir o projeto. Na barra lateral esquerda, você
vai ver todos os arquivos.

---

## ✅ ETAPA 6 — Verificar o .gitignore antes de qualquer commit

Este passo é **crítico para a segurança**. Vamos confirmar que o
arquivo `.env` (com suas senhas) não vai para o GitHub.

1. No VS Code, abra o **Terminal integrado**:
   - Pressione `` Ctrl+` `` (a tecla do acento grave, abaixo do Esc)
   - Ou vá em **Terminal → New Terminal**

2. Certifique-se de que você está na pasta correta. O terminal deve
   mostrar algo como `aed-studio>` ou `/home/seu-usuario/aed-studio`.
   Se não estiver, rode:
   ```bash
   # Linux/macOS
   cd ~/pasta-onde-voce-salvou/aed-studio

   # Windows
   cd C:\pasta-onde-voce-salvou\aed-studio
   ```

3. Inicialize o Git nesta pasta (faz isso apenas uma vez):
   ```bash
   git init
   ```
   Você deve ver a mensagem: `Initialized empty Git repository in ...`

4. Agora confirme que o `.env` está sendo ignorado corretamente:
   ```bash
   git status
   ```
   O arquivo `.env` **NÃO deve aparecer** na lista. Se ele aparecer,
   pare aqui e verifique se o `.gitignore` está na raiz do projeto.

---

## 📝 ETAPA 7 — Fazer o primeiro commit (pela interface visual)

Conventional Commits é um padrão para escrever mensagens de commit
que deixa o histórico do projeto legível. O formato é:
```
tipo(escopo): descrição curta
```

Exemplos de tipos:
- `feat` → nova funcionalidade
- `fix` → correção de bug
- `docs` → mudança na documentação
- `chore` → configuração, build, sem mudança no código

### Usando a extensão visual (recomendado para iniciantes)

1. No VS Code, clique no ícone de **controle de versão** na barra
   lateral esquerda — parece um Y com três bolinhas (ou pressione
   `Ctrl+Shift+G`)

2. Você vai ver uma lista de todos os arquivos com um `U` ao lado
   (Untracked = ainda não rastreados pelo Git).

3. Passe o mouse sobre a linha **"Changes"** e clique no ícone **`+`**
   que aparece à direita (isso seleciona todos os arquivos de uma vez).
   Os arquivos vão mover para a seção **"Staged Changes"**.

   > 📖 **O que é "Staged"?** É como preparar os itens para uma caixa
   > antes de lacrar. Você escolhe o que entra neste commit.

4. Agora, em vez de digitar no campo "Message" diretamente, clique no
   ícone de **raio** (✨) que aparece na barra superior da aba Source
   Control — esse é o botão da extensão Conventional Commits.

5. Um painel vai se abrir pedindo:
   - **Type:** selecione `feat`
   - **Scope:** digite `initial` (ou deixe em branco)
   - **Description:** `estrutura inicial do projeto AED Studio`
   - As outras opções (breaking change, body, footer) deixe em branco
   - Clique em **"Create Commit"**

6. A mensagem que vai ser gerada é:
   ```
   feat(initial): estrutura inicial do projeto AED Studio
   ```

7. Clique no botão azul **"Commit"** para confirmar.

### Alternativa: via terminal (caso prefira)

Se quiser usar o terminal em vez da extensão:
```bash
# Adiciona todos os arquivos
git add .

# Cria o commit com a mensagem Conventional Commits
git commit -m "feat(initial): estrutura inicial do projeto AED Studio"
```

---

## 🔗 ETAPA 8 — Conectar ao repositório do GitHub e enviar

Agora você vai dizer ao Git local onde ficam as "fotos" na nuvem.

### 8.1 — Configurar seu nome no Git (apenas uma vez por máquina)

No terminal do VS Code:
```bash
git config --global user.name "Adriano Lucas"
git config --global user.email "seu@email.com"
```
Substitua pelo seu nome e o e-mail que usou no GitHub.

### 8.2 — Adicionar o repositório remoto

Copie o endereço do repositório que o GitHub mostrou quando você
o criou (termina em `.git`) e rode:

```bash
git remote add origin https://github.com/SEU-USUARIO/aed-studio.git
```

> 📖 **O que faz esse comando?**
> Ele cria um "atalho" chamado `origin` que aponta para o seu
> repositório no GitHub. Toda vez que você quiser enviar código,
> você usa esse atalho.

### 8.3 — Definir o nome da branch principal

```bash
git branch -M main
```

> 📖 **Por quê?** O GitHub usa `main` como nome padrão para a branch
> principal desde 2020. Este comando garante que o nome local bate
> com o que o GitHub espera.

### 8.4 — Enviar o código para o GitHub

```bash
git push -u origin main
```

O terminal vai pedir autenticação:
- **Username:** seu nome de usuário do GitHub
- **Password:** cole o **Personal Access Token** (ghp_...) que você
  salvou no Bitwarden — não a sua senha do GitHub!

> 📖 **O que é `-u origin main`?** O `-u` configura o "rastreamento"
> para que nos próximos envios você só precise digitar `git push`,
> sem precisar repetir `origin main`.

---

## ✅ ETAPA 9 — Confirmar que está tudo certo

1. Acesse seu repositório no GitHub:
   `https://github.com/SEU-USUARIO/aed-studio`

2. Verifique que o repositório mostra o cadeado 🔒 ao lado do nome
   — isso confirma que ele é **privado**.

3. Clique na aba **"Settings"** do repositório e procure por
   **"Danger Zone"** no final da página. Confirme que aparece
   "Change repository visibility" com opção para tornar público —
   isso prova que ele está privado agora.

4. Navegue pelos arquivos e confirme:
   - [ ] `README.md` aparece renderizado na página inicial
   - [ ] `LICENSE` está presente
   - [ ] `.gitignore` está presente
   - [ ] `.env` **NÃO está** na lista de arquivos
   - [ ] A pasta `backend/` com `pom.xml` está visível

---

## 🔁 Como fazer commits no dia a dia (depois deste primeiro)

Toda vez que você modificar algo no projeto e quiser salvar
uma "foto" nova:

**Pela interface visual do VS Code:**
1. Clique no ícone de controle de versão (`Ctrl+Shift+G`)
2. Clique no `+` em "Changes" para preparar os arquivos
3. Use o botão de raio (✨) para escrever a mensagem
4. Clique em "Commit"
5. Clique em "Sync Changes" (botão azul) para enviar ao GitHub

**Ou pelo terminal:**
```bash
git add .
git commit -m "fix(auth): corrigir validação de token expirado"
git push
```

### Exemplos de mensagens para este projeto:
```bash
feat(auth): adicionar endpoint de recuperação de senha
fix(progress): corrigir cálculo de streak ao mudar de fuso horário
docs(readme): adicionar screenshots da plataforma
chore(deps): atualizar Spring Boot para 3.2.4
refactor(service): extrair lógica de XP para classe separada
test(auth): adicionar testes para refresh token rotation
```

---

## 🆘 Problemas comuns e como resolver

### "Permission denied" ou "Authentication failed"
→ Você usou a senha do GitHub em vez do Personal Access Token.
   Copie o token do Bitwarden e use como "Password".

### "fatal: not a git repository"
→ Você está na pasta errada. Verifique com `pwd` (Linux/Mac) ou
   `cd` (Windows) se está dentro de `aed-studio/`.

### O arquivo .env aparece na lista do git status
→ Verifique se o `.gitignore` está na **raiz** do projeto (dentro
   de `aed-studio/`, não dentro de `backend/`). O arquivo `.env`
   também deve estar na raiz.

### "src refspec main does not match any"
→ Você ainda não fez nenhum commit. Volte para a Etapa 7.

### "Updates were rejected because the remote contains work"
→ O repositório remoto tem arquivos que o local não tem. Rode:
   ```bash
   git pull origin main --allow-unrelated-histories
   git push origin main
   ```

---

## 📚 Glossário rápido

| Palavra | Significado simples |
|---|---|
| `git init` | Cria uma "máquina do tempo" na pasta atual |
| `git add` | Coloca arquivos na fila para a próxima foto |
| `git commit` | Tira a foto do que está na fila |
| `git push` | Envia as fotos para o GitHub |
| `git pull` | Baixa fotos novas do GitHub para o computador |
| `origin` | Apelido para o repositório no GitHub |
| `main` | Nome da linha principal de desenvolvimento |
| `branch` | Uma linha paralela de desenvolvimento |
| `staged` | Arquivo preparado para entrar no próximo commit |
| `untracked` | Arquivo que o Git ainda não está monitorando |
| `PAT` | Personal Access Token — senha especial para o terminal |
