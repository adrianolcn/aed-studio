# Segurança

## Segredos

Nunca faça commit de:

- `.env`;
- senhas de banco;
- tokens pessoais;
- chaves privadas;
- certificados;
- secrets reais de JWT.

Use `.env.example` apenas como modelo.

## JWT

Use um `JWT_SECRET` forte, preferencialmente gerado com:

```bash
openssl rand -base64 64
```

No PowerShell:

```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

## Sandbox de Código

O modo `local` é adequado para desenvolvimento e testes controlados.

Para uso público, configure:

```dotenv
CODE_SANDBOX_MODE=docker
CODE_SANDBOX_TIMEOUT_SECONDS=2
CODE_SANDBOX_DOCKER_IMAGE=eclipse-temurin:17-jdk
CODE_SANDBOX_DOCKER_CPUS=0.5
CODE_SANDBOX_DOCKER_MEMORY=128m
CODE_SANDBOX_DOCKER_PIDS_LIMIT=64
```

O Docker deve rodar sem rede:

```bash
docker run --rm --network none eclipse-temurin:17-jdk java -version
```

## CORS

Em desenvolvimento, use origens explícitas:

```dotenv
CORS_ORIGINS=http://localhost:5500,http://127.0.0.1:5500
```

Para mobile na rede local, use padrões controlados:

```dotenv
CORS_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*,http://192.168.*.*:*,http://10.*.*.*:*
```

## Relato de Vulnerabilidade

Se encontrar uma falha de segurança, abra uma issue privada ou comunique o mantenedor antes de publicar detalhes.
