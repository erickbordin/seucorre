# SeuCorre

SeuCorre e um SaaS de treinos de corrida com onboarding, geracao de plano, check-in semanal, historico de treinos e adaptacao guiada por regras de negocio no backend.

> Status atual: MVP em desenvolvimento.
> O backend principal esta funcional e coberto por testes. Parte do frontend e de algumas integracoes ainda esta em evolucao.

## O que ja funciona

- cadastro, login e refresh token com JWT
- gating de onboarding antes da area autenticada
- geracao de plano por `preset` ou `ia`, controlada por configuracao
- listagem, pausa e reativacao de planos
- registro de treinos e historico
- check-in semanal com analise de risco
- progresso semanal e consolidacao de sinais de treino
- estrutura inicial de wearables, notificacoes e paywall

## O que ainda nao esta fechado

- sincronizacao real de wearables: Garmin e Strava ainda usam adapters stubados
- preferencias de notificacao: tela existe, endpoint/backend ainda nao
- billing/premium: paywall existe, checkout e backend ainda nao
- frontend duplicado entre `src/front/src/page` e `src/front/src/page/backend`
- falta suite oficial de testes do frontend
- falta versionamento formal de prompts e observabilidade de IA

## Stack

- Backend: Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, Flyway, Redis
- Frontend: React 18, Vite, React Router, React Query, Tailwind
- Banco: PostgreSQL
- IA: provider configuravel com suporte atual a Ollama e Anthropic
- Testes backend: JUnit, Spring Test, Testcontainers

## Estrutura do repositorio

```text
.
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── src/main/java/com/seucorre
├── src/main/resources
├── src/test/java/com/seucorre
└── src/front
```

## Como rodar localmente

### Pre-requisitos

- Java 17
- Node.js 20+ com `npm`
- Docker
- Docker Compose v2 (`docker compose`) ou Compose legado (`docker-compose`)
- portas `5432`, `6379`, `5050`, `8080` e `5173` livres

### 1. Subir a infraestrutura local

Use o comando que existir na sua maquina:

```bash
docker compose up -d db redis pgadmin
```

ou:

```bash
docker-compose up -d db redis pgadmin
```

Servicos locais:

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- pgAdmin: `http://localhost:5050`

Validacao rapida:

```bash
docker ps
```

Voce deve ver os containers `seucorre-db`, `seucorre-redis` e `seucorre-pgadmin` em estado `Up`.

### 2. Subir o backend

O perfil `dev` ja aponta por padrao para `localhost`, entao o caminho mais simples e carregar as variaveis de `.env.example` e iniciar o Spring Boot.

#### Bash / WSL / Linux

```bash
set -a
source .env.example
set +a
./mvnw spring-boot:run
```

Se preferir exportar manualmente:

```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:postgresql://localhost:5432/seucorre_db
export DB_USERNAME=user
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=seucorre_dev_secret
export TREINO_GERACAO_MODE=preset
export TREINO_GERACAO_FALLBACK_TO_IA=false
./mvnw spring-boot:run
```

#### PowerShell

```powershell
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

API local:

- `http://localhost:8080`

Sinal esperado de sucesso:

- backend sobe sem erro de Flyway
- Tomcat inicia na porta `8080`

### 3. Subir o frontend

```bash
cd src/front
npm install
npm run dev
```

Frontend local:

- `http://localhost:5173`

`VITE_API_URL` e opcional em desenvolvimento porque o `vite.config.js` ja faz proxy de `/api` e `/auth` para `http://localhost:8080`. Use `src/front/.env.example` apenas se quiser apontar para outra API.

### 4. Fluxo completo de validacao

1. Suba `db`, `redis` e `pgadmin`.
2. Inicie o backend em outro terminal.
3. Inicie o frontend em outro terminal.
4. Abra `http://localhost:5173`.
5. Use a API em `http://localhost:8080`.

### Problemas comuns

- `docker compose` nao existe: use `docker-compose`.
- erro de conexao no Postgres ou Redis: confirme que os containers estao `Up` e que as portas `5432` e `6379` nao estao ocupadas por outros processos.
- erro de Flyway ao iniciar o backend: normalmente indica que o banco nao subiu ou que `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` nao batem com o `docker-compose.yml`.
- frontend sobe mas a API falha: confirme que o backend esta escutando em `http://localhost:8080`.

## Modos de geracao de plano

### `preset`

Modo padrao do MVP. Hoje cobre:

- objetivos `SAUDE_GERAL`, `COMPLETAR_5K` e `COMPLETAR_10K`
- niveis `INICIANTE` e `INTERMEDIARIO`
- 3 ou 4 dias por semana
- 8 semanas por plano

### `ia`

Modo opcional para gerar o plano via provider configurado.

Variaveis relevantes:

- `IA_PROVIDER=ollama` ou `anthropic`
- `OLLAMA_BASE_URL`
- `OLLAMA_MODEL`
- `ANTHROPIC_API_KEY`

## Testes e validacao

Backend:

```bash
mvn clean compile
./mvnw -B -ntp verify
```

Frontend:

```bash
cd src/front
npm run lint
npm run typecheck
npm run build
```

CI atual:

- roda `./mvnw -B -ntp verify`
- valida o build da imagem Docker do backend

## Roadmap tecnico imediato

- implementar sincronizacao real de wearables
- criar endpoint real de preferencias de notificacao
- conectar billing/checkout ao premium
- consolidar a arvore ativa do frontend
- adicionar testes de frontend para auth, onboarding, plano e check-in
- versionar prompts e medir qualidade/latencia da IA
- reduzir bundle inicial do frontend com code splitting

## Antes de publicar no GitHub

- escolher e adicionar uma `LICENSE`
- adicionar screenshots ou um GIF do fluxo principal
- revisar a descricao do repositorio e topicos do GitHub
- opcional: adicionar `CONTRIBUTING.md`, issue templates e `CODE_OF_CONDUCT.md`

## Observacoes

- O backend e a fonte da verdade das regras de negocio.
- O MVP sobe em `preset` por padrao para nao depender de IA logo no primeiro fluxo.
- Algumas telas do frontend ja existem para a jornada futura, mas nem todas estao conectadas ao backend final ainda.
