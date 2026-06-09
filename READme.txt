Criado por Erick Bordin e Sarah Nour

Como Rodar o Projeto

  ## Pré-requisitos

  Antes de subir o projeto, instale:

  - Docker Desktop
  - Java 17
  - Node.js 20+ com npm
  - Git

  Confirme no terminal:

  java -version
  node -v
  npm -v
  docker -v

  ## 1. Subir a infraestrutura

  Abra o Docker Desktop.

  Depois, no PowerShell:

  cd C:\Users\KABUM\Desktop\seucorre
  docker start seucorre-db seucorre-redis seucorre-pgadmin

  Se os containers ainda não existirem, crie com:

  cd C:\Users\KABUM\Desktop\seucorre
  docker run --name seucorre-db -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=seucorre_db -p
  5432:5432 -d postgres:15-alpine
  docker run --name seucorre-redis -p 6379:6379 -d redis:7-alpine
  docker run --name seucorre-pgadmin -e PGADMIN_DEFAULT_EMAIL=admin@seucorre.com -e PGADMIN_DEFAULT_PASSWORD=admin -p
  5050:80 -d dpage/pgadmin4

  ## 2. Subir o backend

  O backend precisa receber as variáveis de conexão com Postgres e Redis.

  No PowerShell:

  cd C:\Users\KABUM\Desktop\seucorre

  $env:SPRING_PROFILES_ACTIVE="dev"
  $env:DB_URL="jdbc:postgresql://localhost:5432/seucorre_db"
  $env:DB_USERNAME="user"
  $env:DB_PASSWORD="password"
  $env:REDIS_HOST="localhost"
  $env:REDIS_PORT="6379"
  $env:ANTHROPIC_API_KEY="mock-key"
  $env:IA_PROVIDER="ollama"
  $env:OLLAMA_BASE_URL="http://localhost:11434/api/generate"
  $env:OLLAMA_MODEL="llama3.2"
  $env:JWT_SECRET="seucorre_dev_secret"

  .\mvnw.cmd spring-boot:run

  Se subir corretamente, o backend ficará disponível em:

  http://localhost:8080

  ## 3. Subir o frontend

  Abra outro terminal PowerShell e rode:

  cd C:\Users\KABUM\Desktop\seucorre\src\front
  npm install
  npm run dev

  Se subir corretamente, o frontend ficará disponível em:

  http://localhost:5173

  ## 4. Acessos do projeto

  Após subir tudo:

  - Frontend: http://localhost:5173
  - Backend: http://localhost:8080
  - pgAdmin: http://localhost:5050

  Credenciais do pgAdmin:

  - Email: admin@seucorre.com
  - Senha: admin

  ## 5. Fluxo esperado no sistema

  Ao abrir o frontend:

  1. Acesse /entrar
  2. Se não tiver conta, clique em Criar uma conta
  3. O cadastro cria apenas a conta
  4. Depois do login, o sistema direciona para o onboarding
  5. Só após completar o onboarding o usuário acessa home, plano e geração de treino

  ## 6. Como parar o projeto

  Para parar o backend:

  - pressione Ctrl + C no terminal do Spring Boot

  Para parar o frontend:

  - pressione Ctrl + C no terminal do Vite

  Para parar os containers:

  docker stop seucorre-db seucorre-redis seucorre-pgadmin

  ## 7. Problemas comuns

  ### vite is not recognized

  Rode:

  cd C:\Users\KABUM\Desktop\seucorre\src\front
  npm install

  ### Frontend sobe, mas não abre no navegador

  Confirme que o terminal mostrou:

  Local: http://localhost:5173/

  ### Backend não conecta no banco

  Confirme se o Postgres está rodando:

  docker ps

  E veja se a porta 5432 está exposta.

  ### Erro de Java

  Confirme que está usando Java 17:
  java -version

  ### IA Llama/Ollama não responde

  O projeto usa o provider `ollama` no perfil `dev`.

  Antes de subir o backend, confirme que o Ollama está instalado e em execução no Windows:

  ollama list

  Se o comando não existir, você só baixou o instalador e ainda precisa concluir a instalação.

  Se o modelo llama ainda não existir localmente, rode:

  ollama pull llama3.2

para usar o openclaude
1- docker attach openclaude-java
2- docker ps
3- docker exec -it nomedodocker bash
4- openclaude

