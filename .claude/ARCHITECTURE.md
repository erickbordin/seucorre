# SeuCorre Architecture

## Visao geral

SeuCorre e um SaaS de treinos de corrida com geracao de planos via IA e via catalogo preset para o MVP. O sistema usa um backend monolitico modular em Spring Boot e um frontend SPA em React.

## Stack principal

- Backend: Java 17, Spring Boot 3.2, Spring Web, Validation, JPA, Security, Redis
- Banco: PostgreSQL
- Migracoes: Flyway
- Frontend: React 18, Vite, React Router, React Query, Tailwind
- IA: provider configuravel com Ollama e Anthropic
- Testes backend: JUnit, Spring Test, Testcontainers

## Estrutura do repositorio

- `pom.xml`: configuracao principal do backend
- `src/main/java/com/seucorre`: codigo da aplicacao
- `src/main/resources`: configuracoes e migracoes
- `src/test/java/com/seucorre`: testes backend
- `src/front`: frontend React
- `.claude`: documentacao operacional para humanos e IA

## Arquitetura backend

O backend esta organizado por dominio e por camada.

### Dominios principais

- `usuario`: cadastro, autenticacao, onboarding, perfil e dispositivos
- `treino`: geracao de plano, sessoes, historico e registros
- `avaliacao`: check-in semanal, progresso e risco
- `shared`: enums, exceptions, eventos e value objects
- `infra`: seguranca, IA, cache, notificacoes, scheduler e integracoes

### Camadas principais

- `api`: controllers HTTP
- `application`: orquestracao de casos de uso
- `domain`: regras de negocio
- `infrastructure` ou `infra`: persistencia e adaptadores tecnicos

## Arquitetura de geracao de plano

Fluxo atual de geracao de plano:

1. O controller recebe a requisicao.
2. O service de aplicacao carrega o usuario e o contexto.
3. O modo de geracao e decidido por `seucorre.treino.geracao.mode`.
4. Em `preset`, `PlanoPresetService` carrega um JSON versionado em `src/main/resources/planos-presets`, reaproveita `PlanoTreinoParser` e distribui as datas conforme os dias de treino do usuario.
5. Em `ia`, `PromptBuilder` monta o prompt, `GeradorPlanoIA` chama `IAClient` e o provider concreto responde.
6. O backend valida o resultado parseado e persiste o plano.

Pontos importantes do desenho atual:

- `IAClient` abstrai provider.
- O provider e escolhido por configuracao em `seucorre.ia.provider`.
- O modo inicial do MVP pode ficar em `preset` e voltar para IA por configuracao ou fallback controlado.
- Em dev, o padrao atual de provider continua configuravel.
- A resposta de plano deve voltar em JSON puro, inclusive para reuso do parser com presets.
- Regras criticas continuam no Java, nao apenas no prompt.

## Arquitetura frontend

### Base

- `src/front/src/App.jsx` define as rotas principais.
- `AuthContext` concentra autenticacao e carregamento de usuario.
- `lib/api.js` centraliza chamadas HTTP e normalizacao de payload.
- React Query suporta cache de requisicoes.
- `components/ui` guarda a base de componentes reutilizaveis.

### Estrutura atual relevante

- `src/front/src/page/backend`: paginas atualmente ligadas nas rotas principais
- `src/front/src/page`: paginas legadas ou paralelas, ainda nao consolidadas
- `src/front/src/components/layout`: estrutura visual principal
- `src/front/src/components/home`: componentes da home
- `src/front/src/components/onboarding`: componentes do onboarding

## Fluxos principais do produto

### Autenticacao e acesso

1. Usuario registra conta.
2. Usuario faz login.
3. Frontend salva tokens.
4. Frontend busca `usuarios/me`.
5. Se onboarding nao estiver completo, usuario e redirecionado.

### Geracao de plano

1. Usuario autenticado solicita `POST /api/planos/gerar`.
2. Backend valida usuario e objetivo.
3. IA gera resposta estruturada.
4. Backend parseia, aplica regras e persiste.
5. Frontend normaliza e exibe o plano.

### Check-in e adaptacao

1. Usuario envia check-in semanal.
2. Backend calcula risco e progresso.
3. Se necessario, o plano pode ser reescrito.
4. O historico alimenta novas decisoes.

## Dados e configuracao

- Banco controlado por Flyway.
- Perfil dev usa variaveis de ambiente para banco, Redis, JWT e IA.
- `application.yml` guarda defaults gerais.
- `application-dev.yml` concentra configuracao de desenvolvimento.
- `application-prod.yml` deve refletir defaults seguros para producao.

## Riscos arquiteturais atuais

- Frontend com duas arvores de paginas ainda nao unificadas.
- Baixa visibilidade de testes automatizados no frontend.
- Falta de versionamento formal de prompt.
- Falta de observabilidade de qualidade da resposta da IA.

## Direcao desejada

- manter o backend como fonte da verdade para regras
- usar a IA como motor de sugestao e composicao
- consolidar frontend em uma unica estrutura de paginas
- aumentar testes de fluxo critico
- instrumentar melhor latencia, falhas e qualidade da IA
