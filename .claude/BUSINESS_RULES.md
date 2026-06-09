# SeuCorre Business Rules

## Objetivo do produto

SeuCorre ajuda corredores a receber, executar e ajustar planos de treino com apoio de IA e sinais do proprio uso do sistema.

## Fluxo principal do usuario

1. Cadastro da conta
2. Login
3. Onboarding
4. Geracao de plano
5. Execucao dos treinos
6. Check-in semanal
7. Acompanhamento de progresso e historico

## Regras de acesso

- Usuario nao autenticado nao pode acessar areas privadas.
- Usuario autenticado sem onboarding completo deve ser direcionado ao onboarding.
- Home privada, plano, geracao de plano, progresso, historico, check-in e wearables exigem autenticacao.

## Regras de onboarding

- Cadastro da conta e onboarding sao etapas separadas.
- O sistema considera o onboarding necessario antes do uso completo do produto.
- Informacoes do perfil alimentam a geracao do plano e a adaptacao posterior.

## Regras de elegibilidade para treino

- O usuario precisa estar apto para treinar.
- Se o backend identificar que o usuario nao esta apto, o plano nao deve ser gerado.
- Informacoes de saude e perfil nao podem ser ignoradas em nome de conveniencia.

## Regras de geracao de plano

- O plano deve ser coerente com o objetivo do corredor.
- O plano deve respeitar disponibilidade semanal e dias preferenciais quando existirem.
- O plano deve considerar historico recente quando disponivel.
- A geracao inicial do MVP pode usar catalogo preset ou IA, conforme configuracao do backend.
- No catalogo preset do MVP, as combinacoes suportadas sao: `SAUDE_GERAL`, `COMPLETAR_5K` e `COMPLETAR_10K`; niveis `INICIANTE` e `INTERMEDIARIO`; 3 ou 4 dias por semana; 8 semanas de plano.
- Se a combinacao solicitada nao existir no catalogo preset, o backend deve falhar de forma explicita ou usar fallback para IA quando isso estiver habilitado.
- A saida da IA nao pode introduzir tipos fora do dominio aceito.

## Tipos de treino aceitos no fluxo atual

- `REGENERATIVO`
- `INTERVALADO`
- `LONGO`
- `FARTLAKE`

Se novos tipos forem aceitos, isso deve ser alterado no dominio, nos prompts, no parser e no frontend.

## Regra de seguranca de carga

- O backend aplica a regra dos 10 por cento.
- A progressao semanal nao deve ultrapassar o limite calculado sem ajuste.
- Se a IA gerar volume acima do aceitavel, o backend reduz o volume da semana.

## Regras de reescrita de plano

- Reescrita so deve ocorrer quando o check-in indicar necessidade.
- A reescrita deve priorizar seguranca, recuperacao e continuidade.
- Se nao houver necessidade de reescrita, o plano atual deve ser mantido.

## Regras de check-in e progresso

- Check-in semanal serve para captar fadiga, risco e aderencia.
- Progresso semanal ajuda a contextualizar ajustes.
- Historico e sinais recentes devem influenciar a adaptacao do plano.

## Regras de persistencia

- Apenas respostas validadas devem ser persistidas.
- Schema do banco e controlado por migracoes Flyway.
- Mudancas de regra que alterem dados devem refletir em migration e documentacao.

## Regras de autenticacao

- O frontend usa token de acesso e refresh token.
- Requisicoes protegidas devem carregar bearer token.
- Em caso de 401, o frontend tenta refresh uma vez antes de falhar.

## Regras de integracao com dispositivos

- Dispositivos externos fazem parte do contexto do usuario.
- Sincronizacao deve respeitar plataforma e regras de autenticacao da integracao.
- Dados de wearable nao podem substituir validacoes de dominio.

## Regra de verdade do sistema

- regra de negocio vive no backend
- UI apresenta o estado
- IA sugere e estrutura
- banco persiste somente resultado validado
