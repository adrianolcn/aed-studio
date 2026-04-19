# Contribuindo

Obrigado pelo interesse no AED Studio.

Este projeto prioriza consistência entre produto educacional, back-end e front-end. Antes de abrir uma contribuição, mantenha estas regras em mente:

- O back-end é a fonte de verdade para progresso, XP, tópicos, exercícios, analytics e recomendações.
- Evite estado local no front quando o dado deve ser persistido.
- Não duplique IDs de tópicos fora do catálogo central.
- Toda nova funcionalidade deve ter teste proporcional.
- Documente novos endpoints ou mudanças de contrato.

## Como Rodar Antes de Contribuir

Back-end:

```bash
cd backend
./mvnw test
```

Windows:

```powershell
cd backend
.\mvnw.cmd test
```

Front-end:

```bash
npm install
npm run test:frontend:contracts
```

E2E:

```bash
npm run playwright:install
npm run test:e2e
```

## Padrão de Commit

Use mensagens no estilo Conventional Commits:

```text
feat(code): persist code submissions
fix(frontend): keep sandbox feedback visible
docs(readme): improve setup guide
test(e2e): cover code judge flow
```

## Checklist de Pull Request

- [ ] Testes de back-end passam.
- [ ] Testes contratuais do front passam.
- [ ] E2E passa quando a mudança afeta fluxo principal.
- [ ] README/docs atualizados se houver mudança de uso.
- [ ] Nenhum segredo foi commitado.
- [ ] Nenhum artefato local foi incluído.
