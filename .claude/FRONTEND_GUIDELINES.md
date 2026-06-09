# SeuCorre Frontend Guidelines

## Objetivo

Manter o frontend consistente, legivel, mobile-first e conectado ao dominio real do produto.

## Stack observada

- React 18
- Vite
- React Router
- React Query
- Tailwind
- componentes reutilizaveis em `components/ui`

## Estrutura recomendada

### Rotas e paginas

- Usar `App.jsx` como referencia oficial de fluxo.
- Tratar `src/page/backend` como arvore principal das rotas ativas atuais.
- Evitar criar novas paginas duplicadas em `src/page` e `src/page/backend`.
- Se uma tela for consolidada, remover ou descontinuar a versao paralela.

### Estado e dados

- Autenticacao fica em `AuthContext`.
- Chamadas HTTP devem passar por `lib/api.js`.
- Normalizacao de payload tambem deve ficar centralizada em `lib/api.js` ou utilitario dedicado.
- Evitar `fetch` direto dentro de pagina ou componente quando ja existe camada de API.

### Componentes

- Componentes de dominio vao em pastas como `components/home` e `components/onboarding`.
- Componentes de base vao em `components/ui`.
- Layout global fica em `components/layout`.

## Regras de UX

- Toda pagina deve ter estado de loading.
- Toda pagina com dados remotos deve ter estado de erro.
- Toda lista ou area dependente de dados deve ter estado vazio.
- Rotas protegidas devem lidar bem com redirecionamento.
- Mobile e prioridade, desktop e adaptacao.

## Regras de design

Base atual:

- tokens visuais estao em `src/index.css`
- tema atual usa fundo escuro com acento verde lima

Manter:

- consistencia de espacamento
- contraste legivel
- hierarquia visual clara
- CTA principal evidente

Evitar:

- paginas visualmente desconectadas umas das outras
- excesso de efeitos sem funcao
- misturar muitos estilos de cartao, botao e tipografia na mesma jornada

## Padrao de tela

Toda tela relevante deve responder:

- qual a acao principal
- qual o estado atual do usuario
- o que esta carregando
- o que fazer se falhar
- qual o proximo passo esperado

## Formularios

- validar no frontend para feedback rapido
- validar de novo no backend
- mostrar erro por campo quando possivel
- usar textos curtos e objetivos

## Integracao com backend

- usar os endpoints centralizados em `api`
- manter nomes coerentes com o dominio
- quando o backend mudar contrato, atualizar normalizadores e telas afetadas

## Qualidade

Pontos prioritarios para evolucao:

- criar testes de fluxo para auth e onboarding
- criar testes de tela para geracao de plano e visualizacao do plano
- consolidar a arvore de paginas
- reduzir logica de negocio dentro da camada visual

## Checklist antes de aceitar mudanca visual

- funciona em mobile
- funciona em desktop
- loading existe
- erro existe
- vazio existe
- rota protegida continua correta
- o texto da tela combina com a regra do produto
