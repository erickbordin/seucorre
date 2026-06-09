# SeuCorre Decisions

Este arquivo registra decisoes tecnicas aceitas e pontos que ainda precisam de fechamento.

## Decisoes aceitas

### D-001

- Status: aceita
- Tema: arquitetura backend
- Decisao: usar backend monolitico modular em Spring Boot
- Motivo: reduz complexidade operacional e permite separar dominios sem fragmentar deploy cedo demais

### D-002

- Status: aceita
- Tema: frontend
- Decisao: usar SPA em React com Vite
- Motivo: desenvolvimento rapido, boa experiencia para fluxo autenticado e integracao simples com API propria

### D-003

- Status: aceita
- Tema: persistencia
- Decisao: usar PostgreSQL com Flyway
- Motivo: banco relacional combina com o dominio e Flyway controla evolucao do schema

### D-004

- Status: aceita
- Tema: integracao de IA
- Decisao: isolar provider por meio da interface `IAClient`
- Motivo: permite trocar provider sem contaminar a regra de negocio

### D-005

- Status: aceita
- Tema: formato de saida da IA
- Decisao: exigir JSON puro para resposta estruturada de plano
- Motivo: facilita parse, validacao e persistencia

### D-006

- Status: aceita
- Tema: regras criticas
- Decisao: manter regras de negocio criticas no backend
- Motivo: modelo pode errar, variar ou ignorar instrucoes

### D-007

- Status: aceita
- Tema: seguranca de carga
- Decisao: aplicar regra dos 10 por cento apos a resposta da IA
- Motivo: garantir seguranca mesmo quando a geracao vier otimista demais

### D-008

- Status: aceita
- Tema: autenticacao frontend
- Decisao: centralizar tokens, refresh e request wrapper em `lib/api.js`
- Motivo: evita duplicacao e melhora controle de erros

### D-009

- Status: aceita
- Tema: fluxo de acesso
- Decisao: exigir onboarding antes do uso completo da area autenticada
- Motivo: o sistema depende dos dados do perfil para gerar planos coerentes

### D-010

- Status: aceita
- Tema: arvore de rotas atual
- Decisao: considerar `src/front/src/page/backend` como base das rotas ativas atuais
- Motivo: `App.jsx` aponta principalmente para essa arvore hoje

### D-011

- Status: aceita
- Tema: geracao de plano no MVP
- Decisao: permitir modo `preset` com catalogo versionado de planos como caminho principal do MVP, mantendo IA como modo configuravel e fallback opcional
- Motivo: reduz custo, risco operacional e dependencias externas no inicio, sem quebrar o fluxo futuro com IA

## Pontos abertos

### O-001

- Status: aberto
- Tema: testes frontend
- Questao: qual stack oficial de testes sera adotada no frontend
- Sugestao: Vitest + Testing Library para fluxo de auth, onboarding e plano

### O-002

- Status: aberto
- Tema: observabilidade de IA
- Questao: onde registrar latencia, erro, provider e qualidade de resposta
- Sugestao: criar metrica padrao por chamada e log estruturado

### O-003

- Status: aberto
- Tema: prompts
- Questao: como versionar prompts e avaliar regressao
- Sugestao: criar pasta dedicada de prompts e conjunto de casos de avaliacao

### O-004

- Status: aberto
- Tema: consolidacao frontend
- Questao: quando remover ou unificar paginas duplicadas entre `page` e `page/backend`
- Sugestao: tratar isso como tarefa de arquitetura de frontend antes de expandir muito a UI

### O-005

- Status: aberto
- Tema: design system
- Questao: ate onde os componentes atuais sao regra oficial e ate onde sao apenas base de implementacao
- Sugestao: promover tokens, componentes e padroes de pagina a um design system leve
