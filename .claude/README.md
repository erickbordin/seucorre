# SeuCorre AI Docs

Esta pasta centraliza o contexto operacional do projeto para humanos e assistentes de IA.

## Objetivo

- reduzir ambiguidade
- manter consistencia tecnica
- registrar regras importantes do produto
- acelerar implementacoes sem perder controle

## Como usar esta pasta

Leia os arquivos nesta ordem quando for iniciar uma mudanca no projeto:

1. `README.md`
2. `ARCHITECTURE.md`
3. `BUSINESS_RULES.md`
4. `AI_GUIDELINES.md`
5. `FRONTEND_GUIDELINES.md`
6. `DECISIONS.md`

## Regra de operacao

- Regra de negocio critica nao deve existir apenas em prompt.
- Toda mudanca relevante no produto deve refletir em codigo, testes e documentacao.
- Se a documentacao divergir do comportamento atual, o codigo e os testes sao a fonte da verdade imediata.
- Depois de resolver a divergencia, atualize os arquivos desta pasta.

## O que existe hoje no projeto

- Backend principal em Spring Boot 3 com Java 17.
- Frontend em React 18 com Vite.
- Persistencia em PostgreSQL com Flyway.
- Redis para suporte de infraestrutura, principalmente rate limiting.
- Integracao de IA com provider configuravel.
- Fluxo principal do produto: cadastro, login, onboarding, geracao de plano, treino, check-in, progresso e historico.

## Arquivos desta pasta

- `ARCHITECTURE.md`: stack, modulos, camadas e fluxos principais.
- `BUSINESS_RULES.md`: regras essenciais do dominio SeuCorre.
- `AI_GUIDELINES.md`: como projetar, testar e manter funcionalidades com IA.
- `FRONTEND_GUIDELINES.md`: padroes de UX, design e organizacao do frontend.
- `DECISIONS.md`: decisoes tecnicas aceitas e pontos abertos.

## Fluxo recomendado ao usar IA no SeuCorre

1. Pedir analise do impacto antes de editar.
2. Limitar o escopo da tarefa para 1 objetivo por vez.
3. Pedir implementacao com arquivos alvo definidos.
4. Pedir revisao focada em regressao, seguranca e testes faltantes.
5. Atualizar esta pasta se a mudanca afetar arquitetura, regra ou fluxo.

## Definicao minima de qualidade

- codigo compila
- comportamento principal continua funcionando
- regras criticas ficam no codigo
- erros tem tratamento razoavel
- nomes e estrutura permanecem consistentes
- documentacao importante foi atualizada
