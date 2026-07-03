# Deploy do SeuCorre

Este runbook cobre o deploy do MVP atual sem provider externo de IA.
O backend roda em modo manual/preset e o frontend e servido por Nginx com proxy para a API.

## Arquivos usados

- `docker-compose.prod.yml`
- `.env.production`
- `Dockerfile`
- `src/front/Dockerfile`
- `src/front/nginx.conf`

## Premissas

- Docker instalado no host
- um runner de Compose disponivel no host
- DNS do frontend apontando para o host publico
- TLS terminado em proxy reverso ou load balancer na frente do container `frontend`

## 1. Preparar variaveis

Crie o arquivo `.env.production` a partir do template:

```bash
cp .env.production.example .env.production
```

Revise no minimo:

- `JWT_SECRET`
- `DB_PASSWORD`
- `POSTGRES_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `FRONTEND_PORT`

Valores esperados neste modo:

- `SPRING_PROFILES_ACTIVE=prod`
- `TREINO_GERACAO_MODE=preset`
- `TREINO_GERACAO_FALLBACK_TO_IA=false`
- `IA_PROVIDER=manual`
- `SERVER_SSL_ENABLED=false`

## 2. Subir a stack

Com Compose:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```

Se o seu ambiente usa binario legado:

```bash
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```

A stack sobe:

- `frontend`: Nginx servindo a SPA na porta `FRONTEND_PORT`
- `backend`: Spring Boot na rede interna do Compose
- `db`: PostgreSQL 15 com volume persistente
- `redis`: Redis 7 com volume persistente

## 3. Publicacao com TLS

O aplicativo foi preparado para rodar com TLS fora do container Spring Boot.
Mantenha o proxy reverso ou load balancer terminando HTTPS e encaminhando trafego HTTP para o container `frontend`.

Cabecalhos esperados:

- `X-Forwarded-Proto`
- `X-Forwarded-For`
- `Host`

## 4. Smoke test

Verifique containers:

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

Teste o frontend:

```bash
curl -I http://SEU_HOST/
```

Resultado esperado:

- `200 OK`

Teste o proxy para a API em rota protegida:

```bash
curl -i http://SEU_HOST/api/usuarios
```

Resultado esperado:

- `401 Unauthorized`

Esse `401` confirma que:

- Nginx esta no ar
- o proxy `/api` esta funcional
- o backend respondeu
- a seguranca JWT esta ativa

## 5. Logs

Frontend:

```bash
docker logs --tail 100 seucorre-frontend
```

Backend:

```bash
docker logs --tail 100 seucorre-backend
```

Banco:

```bash
docker logs --tail 100 seucorre-db
```

## 6. Rollback simples

Se o problema vier de uma nova imagem local:

1. volte o codigo para a revisao anterior no host
2. rode novamente o `up -d --build`

Se o problema vier apenas de configuracao:

1. ajuste `.env.production`
2. reaplique:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production up -d
```

## 7. Observacoes importantes

- O deploy atual nao depende de IA externa.
- O frontend continua com a UI atual; este runbook nao assume nenhuma mudanca visual.
- O projeto ainda nao possui endpoint dedicado de health check.
- O frontend gera um bundle grande; isso nao bloqueia deploy, mas ainda merece otimizacao posterior.
