# SeuCorre AI Guidelines

## Objetivo

Usar IA para gerar e adaptar planos de treino com seguranca, consistencia e previsibilidade de produto.

## Principio central

IA e um componente do sistema, nao a regra principal do sistema.

Isso significa:

- a IA pode sugerir estrutura e texto
- o backend valida, ajusta e decide o que persiste
- restricoes criticas nao podem ficar apenas no prompt

## Implementacao atual

Pontos principais do codigo:

- `IAClient`: contrato de provider
- `OllamaClient`: provider local ou de desenvolvimento
- `AnthropicClient`: provider alternativo
- `PromptBuilder`: composicao de prompt
- `GeradorPlanoIA`: orquestracao, parse e aplicacao de regra pos-resposta
- `PlanoTreinoParser`: conversao do JSON para o dominio

## Regras obrigatorias para features com IA

- Toda resposta persistida deve ter formato validavel.
- Sempre preferir JSON puro para resposta estruturada.
- O backend deve rejeitar ou corrigir resposta invalida.
- Timeout, retry e erro amigavel devem existir no adaptador do provider.
- Regra de negocio critica deve continuar no Java.
- Logs devem permitir identificar provider, tempo, falha e payload relevante sem vazar segredo.

## O que a IA pode fazer no SeuCorre

- gerar plano inicial
- reescrever plano com base em check-in e progresso
- sugerir ajuste textual
- explicar risco ou orientar o corredor com linguagem natural

## O que a IA nao deve decidir sozinha

- se o usuario esta apto para treinar
- se uma regra de seguranca pode ser ignorada
- se um dado inconsistente deve ser persistido
- se um tipo de treino fora do dominio deve ser aceito

## Formato de saida

Para saidas estruturadas:

- responder apenas JSON
- sem markdown
- sem comentario
- sem texto antes ou depois

Para o fluxo de plano, o backend espera estrutura compatvel com:

- objeto `plano`
- `resumoIA`
- `guiaTiposTreino`
- lista `sessoes`

## Regras de prompt

- prompts devem ser pequenos, especificos e orientados a estrutura
- incluir contexto do corredor, mas evitar excesso inutil
- deixar claro o formato de resposta esperado
- proibir texto fora do schema
- orientar seguranca, progressao e aderencia ao objetivo

## Regras pos-IA

Depois da resposta do provider:

- parsear JSON
- validar tipos e campos obrigatorios
- aplicar regra dos 10 por cento
- ordenar sessoes
- preencher metadados derivados
- persistir apenas se o resultado final estiver coerente

## Provider e ambiente

Configuracao atual observada:

- `seucorre.ia.provider`: seleciona o provider
- dev usa `ollama` por padrao
- Anthropic tambem esta configurado

Nunca acople a regra de negocio a um provider especifico.

## Avaliacao de qualidade da IA

Toda mudanca em prompt, parser ou provider deve ser avaliada com casos fixos.

Minimo recomendado:

- corredor iniciante
- corredor intermediario
- baixa disponibilidade semanal
- check-in com dor ou fadiga
- resposta invalida do modelo
- timeout do provider

## Metricas que o projeto deveria registrar

- provider utilizado
- modelo utilizado
- latencia por chamada
- taxa de erro
- taxa de parse invalido
- quantidade de reescritas de plano
- porcentagem de planos ajustados pelo backend apos a resposta

## Checklist antes de subir mudanca com IA

- o prompt ficou mais claro, nao apenas maior
- a saida continua estruturada
- existe validacao no backend
- erros de provider geram resposta controlada
- o fluxo foi coberto por teste
- a documentacao foi atualizada

## Anti-patterns

- confiar que o modelo sempre segue instrucoes
- colocar regra critica apenas em linguagem natural
- salvar resposta da IA sem validacao
- misturar texto explicativo com JSON
- medir sucesso por impressao subjetiva apenas
