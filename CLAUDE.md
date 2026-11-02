# SeuCorre

App de treino de corrida: gera o plano (catálogo preset ou IA), registra treinos e ajusta o plano pelo check-in semanal. Backend Java 17 / Spring Boot 3.2 / PostgreSQL + Flyway / Redis (rate limit). Frontend React 18 + Vite + React Query + Tailwind, mobile-first, com destino App Store e Google Play.

## Fonte da verdade

O backend decide; a IA sugere e estrutura; a UI apresenta; o banco guarda só resultado validado.

Guardrails vivem no Java, nunca só no prompt:
- **Regra dos 10%**: `RegraDezPorCento`, aplicada depois da resposta da IA em `GeradorPlanoIA` (corta a semana, arredondando para baixo).
- **Risco crítico** na reescrita: primeira semana nova com no máximo 70% do volume de referência e `INTERVALADO` vira `REGENERATIVO`.
- **Tipos de treino**: `REGENERATIVO`, `INTERVALADO`, `LONGO`, `FARTLAKE` (essa grafia; a UI mostra "Fartlek"). Tipo novo exige mudar enum, prompt, parser e `TYPE_METADATA` em `src/front/src/lib/api.js`.
- Aptidão para treinar, regra de segurança, dado inconsistente e tipo fora do domínio são decisões do backend.

## Mapa

- Backend `src/main/java/com/seucorre`: `usuario` (auth, onboarding, perfil), `treino` (plano, sessões, registros, IA), `avaliacao` (check-in, progresso, risco), `shared`, `infra` (segurança, providers de IA, notificações, scheduler). Camadas `api` → `application` → `domain` → `infrastructure`.
- Presets: `src/main/resources/planos-presets/<objetivo>/<nivel>/<3|4>-dias.json`, com `saude_geral`, `completar_5k` e `completar_10k` (8 semanas) e `completar_meia_maratona` (12 semanas, a última sessão é a prova) × `iniciante` e `intermediario`. `AVANCADO` usa o plano `intermediario`. Objetivo sem preset aparece como "em breve" no onboarding (`soon` em `RUNNER_GOALS`).
- Frontend `src/front/src`: rotas em `App.jsx`; telas ativas em `page/backend` (`page/` é legado, exceto `page/Paywall.jsx`); HTTP, refresh de token e normalização de payload em `lib/api.js`; gate de auth e onboarding em `lib/AuthContext.jsx`; componentes por domínio em `components/<dominio>`, base em `components/ui`, casca em `components/layout`.
- Eval da IA: `eval/rodar_eval.py` + `eval/casos.json`, resultados versionados em `eval/resultados/`.

## Fluxos que não podem quebrar

- **Acesso**: visitante → login ou cadastro → `GET /api/usuarios/me` → sem onboarding vai para `/onboarding` → depois as rotas privadas. Um 401 tenta refresh uma vez em `lib/api.js` antes de encerrar a sessão.
- **Login com Google**: o botão oficial (Google Identity Services, `components/auth/GoogleSignInButton.jsx`) entrega um ID token; `POST /auth/google` valida assinatura, emissor, audiência e validade em `GoogleIdTokenVerifier` e entra pela mesma conta do e-mail verificado (`google_id` vinculado). Conta criada pelo Google não tem senha. `GOOGLE_CLIENT_ID` vazio esconde o botão e desliga o endpoint.
- **Geração de plano**: `seucorre.treino.geracao.mode` escolhe `preset` (padrão) ou `ia`; `fallback-to-ia` cobre combinação fora do catálogo, senão a geração falha de forma explícita.
- **Agenda**: `AgendaDeTreino` ignora datas vindas da IA e agenda nos dias de treino do usuário, a partir de hoje, no fuso `America/Sao_Paulo` (bean `Clock`).
- **Data da prova**: opcional no perfil (`dataProva`, `nomeProva`) para 5 km, 10 km e meia. Com data, `agendarAteAProva` põe a última sessão (a prova) no dia e ocupa os dias de treino anteriores; sobra de tempo vira até 4 semanas de base (cópias da semana 1) e, além disso, o plano começa mais tarde; falta de tempo recusa informando a primeira data possível. A reescrita do check-in mantém a última sessão no dia da prova.
- **Check-in**: `CheckinSemanal` calcula o risco. Risco acima de `BAIXO`, provider com IA e semanas pela frente → a reescrita troca, no mesmo plano, só os treinos pendentes depois da semana do check-in; treinos registrados ficam. A análise em texto recebe essa decisão e a explica sem contradizer.

## IA

- Provider por `seucorre.ia.provider` (`IA_PROVIDER`): `manual` (padrão em dev), `ollama`, `anthropic`, `gemini`, todos atrás de `IAClient`. Regra de negócio fica fora dos providers.
- Saída estruturada é JSON puro; `PlanoTreinoParser` recusa JSON truncado, sessão sem semana, distância ≤ 0 e semanas faltando. Texto para o corredor é texto puro, sem markdown.
- Nos clientes Anthropic e Gemini, retry só para falha temporária (429, 5xx, IO); JSON inválido falha na hora.
- Mudou prompt, parser ou provider → rode o eval e compare com `eval/resultados/` antes de abrir PR.
- Gemini free tier: o Google usa os dados enviados para treinar modelos, então só dados de teste (LGPD). Cota baixa: use `--pausa` no eval.

## Frontend

- Toda tela com dados remotos cobre carregando, erro e vazio.
- Visual: tokens em `src/index.css` (fundo escuro, acento verde-limão); cor por tipo de treino e `formatKm` em `lib/workoutTypes.js`; cores de risco em `lib/checkin.js`. Reutilize antes de criar.
- Referência de produto: Runna, com identidade própria (sem marca, escudo ou textos deles).
- Textos em português com acento, prometendo só o que o backend entrega.

## Comandos

- Testes backend: `./mvnw test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false`. Os `*IntegrationTest` usam `host.docker.internal` fixo e quebram no Linux.
- Frontend (`src/front`): `npm run build` é a checagem real; `npm run lint` não cobre `src/page` porque o config aponta para `src/pages`.
- Eval: `python3 eval/rodar_eval.py --saida eval/resultados/<modelo>-<versao>.jsonl` com o backend em `TREINO_GERACAO_MODE=preset`.

## Convenções

- Commits em português no padrão Conventional Commits: `tipo(escopo): descrição em minúsculas`.
- Os arquivos misturam finais de linha CRLF e LF: preserve o de cada arquivo ao editar e confira com `git diff --stat` contra `git diff --ignore-cr-at-eol --stat`.

## Decisões em aberto

- Monetização: semana 1 grátis com paywall depois, ou paywall desde o início. Nada implementado.
- Testes de frontend: Vitest + Testing Library (hoje não há nenhum).
- Consolidar `src/page` (legado) em `page/backend`.
- Observabilidade da IA: latência, erro, provider e taxa de parse inválido por chamada.

Mudou regra, fluxo ou arquitetura → atualize este arquivo no mesmo PR. Se ele divergir do código, código e testes valem.
