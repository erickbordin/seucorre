# SeuCorre

Guia rapido para subir o projeto localmente.

## Stack do projeto

- Backend: Spring Boot 3.2 + Java 17
- Frontend: React 18 + Vite
- Banco: PostgreSQL
- Cache e rate limiting: Redis
- Infra local: Docker Compose
- Geracao de plano no MVP: `preset` por padrao

## Pre-requisitos

Instale antes de começar:

- Java 17
- Node.js 20+ com npm
- Docker Desktop
- Git
- Ollama apenas se quiser testar fluxos com IA local

Confira no terminal:

```powershell
java -version
node -v
npm -v
docker -v
```

## 1. Subir a infraestrutura

Na raiz do projeto:

```powershell
cd C:\Users\KABUM\Desktop\seucorre
docker compose up -d db redis pgadmin
```

Se quiser derrubar depois:

```powershell
docker compose down
```

Servicos expostos:

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- pgAdmin: `http://localhost:5050`

Credenciais do pgAdmin:

- Email: `admin@seucorre.com`
- Senha: `admin`

## 2. Subir o backend

### Windows PowerShell

O backend usa o perfil `dev`. Para rodar no Windows, use `localhost` nas conexoes locais.

> Importante: os comandos com `$env:` funcionam somente no **PowerShell**.
> Se o seu terminal mostra algo como `PS C:\Users\...>`, voce esta no PowerShell.
> Se ele mostra apenas `C:\Users\...>`, voce esta no **cmd.exe** e esses comandos vao falhar.

```powershell
cd C:\Users\KABUM\Desktop\seucorre

$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:postgresql://localhost:5432/seucorre_db"
$env:DB_USERNAME="user"
$env:DB_PASSWORD="password"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:JWT_SECRET="seucorre_dev_secret"
$env:TREINO_GERACAO_MODE="preset"
$env:TREINO_GERACAO_FALLBACK_TO_IA="false"

.\mvnw.cmd spring-boot:run
```

### Windows CMD

Se voce estiver no `cmd.exe`, use `set` em vez de `$env:`:

```cmd
cd C:\Users\KABUM\Desktop\seucorre

set SPRING_PROFILES_ACTIVE=dev
set DB_URL=jdbc:postgresql://localhost:5432/seucorre_db
set DB_USERNAME=user
set DB_PASSWORD=password
set REDIS_HOST=localhost
set REDIS_PORT=6379
set JWT_SECRET=seucorre_dev_secret
set TREINO_GERACAO_MODE=preset
set TREINO_GERACAO_FALLBACK_TO_IA=false

mvnw.cmd spring-boot:run
```

### WSL ou Linux

Se rodar o backend dentro de WSL/Linux e o Docker estiver no Windows Desktop, prefira `host.docker.internal`:

```bash
cd /desktop/seucorre

export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:postgresql://host.docker.internal:5432/seucorre_db
export DB_USERNAME=user
export DB_PASSWORD=password
export REDIS_HOST=host.docker.internal
export REDIS_PORT=6379
export JWT_SECRET=seucorre_dev_secret
export TREINO_GERACAO_MODE=preset
export TREINO_GERACAO_FALLBACK_TO_IA=false

./mvnw spring-boot:run
```

Quando subir corretamente, a API ficara em:

- `http://localhost:8080`

## 3. Subir o frontend

Em outro terminal:

```powershell
cd C:\Users\KABUM\Desktop\seucorre\src\front
npm install
npm run dev
```

Frontend em:

- `http://localhost:5173`

## 4. Modo MVP atual

O projeto agora sobe em modo `preset` por padrao para geracao inicial de plano. Isso significa que o MVP nao depende de IA para funcionar logo de cara.

Hoje o catalogo inicial cobre:

- `SAUDE_GERAL`
- `COMPLETAR_5K`
- `COMPLETAR_10K`
- niveis `INICIANTE` e `INTERMEDIARIO`
- 3 ou 4 dias por semana
- 8 semanas por plano

Se quiser testar geracao com IA desde o inicio, troque o modo:

```powershell
$env:TREINO_GERACAO_MODE="ia"
```

## 5. Se quiser usar IA local com Ollama

Isso e opcional para o MVP em modo `preset`, mas necessario se voce quiser testar fluxos de IA.

No Windows PowerShell:

```powershell
ollama list
ollama pull llama3.2

$env:IA_PROVIDER="ollama"
$env:OLLAMA_BASE_URL="http://localhost:11434/api/generate"
$env:OLLAMA_MODEL="llama3.2"
```

Se o backend estiver rodando em WSL/Linux, use:

```bash
export IA_PROVIDER=ollama
export OLLAMA_BASE_URL=http://host.docker.internal:11434/api/generate
export OLLAMA_MODEL=llama3.2
```

## 6. Fluxo esperado da aplicacao

Depois de subir tudo:

1. Acesse `/entrar`
2. Se nao tiver conta, crie uma conta
3. Complete o onboarding
4. Gere o plano inicial
5. Acesse home, plano, progresso e check-in

## 7. Como parar

Backend:

- pressione `Ctrl + C` no terminal do Spring Boot

Frontend:

- pressione `Ctrl + C` no terminal do Vite

Infra:

```powershell
docker compose down
```

## 8. Problemas comuns

### Variaveis de ambiente dao erro no Windows

- se apareceu erro ao usar `$env:...`, voce provavelmente executou no `cmd.exe`, nao no PowerShell
- no PowerShell o prompt costuma comecar com `PS C:\...>`
- no `cmd.exe`, use os comandos com `set` mostrados acima

### Backend nao conecta no banco

- confirme que o container `seucorre-db` esta de pe: `docker ps`
- confira se a porta `5432` esta livre e exposta
- se estiver em WSL/Linux, use `host.docker.internal` em vez de `localhost`

### Backend nao conecta no Redis

- confirme que o container `seucorre-redis` esta de pe
- confira a porta `6379`
- se estiver em WSL/Linux, use `host.docker.internal`

### Frontend nao sobe

- rode `npm install` dentro de `src/front`
- confirme a versao do Node.js

### IA local nao responde

- confirme que o Ollama esta em execucao
- teste `ollama list`
- confira o valor de `OLLAMA_BASE_URL`
- lembre que no modo `preset` o MVP nao precisa de IA para gerar plano inicial
