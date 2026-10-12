# Budgeting: controle de gastos com Spring Boot, interface web e IA por voz

Projeto do **Desafio Final do Bootcamp Itaú / DIO**: *Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot*.

> Este repositório evolui o projeto base [`05-spring-ai`](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai) da trilha oficial [dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track).

---

## Visão geral

Aplicação local de controle de gastos composta por:

- **API REST** em Spring Boot para registrar e consultar despesas;
- **MySQL** em Docker para persistência;
- **interface web** servida pelo próprio Spring Boot em `http://localhost:8080/` (HTML, CSS e JavaScript puro);
- **perfil `demo`** que popula um banco separado com dados de demonstração;
- **integração com IA por voz** no back-end (`POST /transactions/ai`), que depende de uma chave da OpenAI com créditos. Esse fluxo ainda **não foi executado** nesta validação (veja [Uso da IA por voz](#uso-da-ia-por-voz-arquitetura-e-estado-atual)).

---

## Funcionalidades validadas

| Funcionalidade | Como foi validada |
|---|---|
| Cadastro de despesa pelo formulário da interface (valor digitado em reais, enviado em centavos) | Interface + `GET` + consulta SQL |
| Consulta e filtro por categoria, com estado vazio | Interface + `GET /transactions/{category}` |
| Valores recebidos em centavos e exibidos em BRL (`8000` → `80.00` → `R$ 80,00`) | REST, interface e testes unitários |
| Cards de total geral, Farmácia, Mercado e quantidade de lançamentos | Interface |
| Banco de demonstração separado (`transaction_demo`), com seed que não duplica | Testes unitários + dois startups reais do perfil `demo` |
| Cálculo de total, quantidade e maior despesa (`summarize-expenses`) | Testes unitários (sem IA) |

Os números reais estão em [Evidências de validação](#evidências-de-validação).

---

## Tecnologias

- Java 25
- Spring Boot 4.0.5 (Web, Data JPA, Docker Compose support)
- Spring AI 2.0.0-M4 com OpenAI. Os modelos abaixo estão **configurados no projeto** (`application.properties`), mas o fluxo de IA **ainda não foi executado nem validado**:
  - `whisper-1` para transcrição de áudio
  - `gpt-4o-mini` para chat e Tool Calling
  - `gpt-4o-mini-tts` para texto em fala
- MySQL 9.6, via Docker Compose
- HTML, CSS e JavaScript puro, servidos pelo Spring Boot (sem framework front-end, npm ou CDN)
- Gradle 9.4.1 (wrapper), Lombok
- JUnit 5 + AssertJ

---

## Arquitetura

Camadas inspiradas em DDD / Clean Architecture:

```
src/main/java/dio/budgeting
├── domain/                 Transaction, Category, TransactionId, TransactionRepository (porta)
├── application/            casos de uso, também expostos como tools (@Tool)
│   ├── input/              PersistTransactionInput
│   └── output/             TransactionOutput, ExpenseSummaryOutput (valores em reais)
└── infrastructure/
    ├── http/               TransactionController, request/response
    ├── persistence/        TransactionEntity, JpaTransactionRepository (adaptador da porta)
    └── demo/               DemoDataSeeder (somente no perfil demo)

src/main/resources
├── static/                 interface web: index.html, css/styles.css, js/app.js, js/money.js
└── prompts/                system-message.st (prompt do assistente de IA)
```

Categorias disponíveis (`Category`): `GROCERIES` (Mercado), `PHARMA` (Farmácia), `AUTO` (Automóvel).

---

## Como executar localmente

### Pré-requisitos

- JDK 25
- Docker em execução
- `OPENAI_API_KEY` **não é necessária** para a API REST nem para a interface. Ela só é exigida pelo endpoint de voz.

### 1. Subir o MySQL

```bash
docker compose up -d
docker compose ps
```

Espere o serviço `database` aparecer como `healthy`. Na **primeira** inicialização, o MySQL pode levar alguns minutos e aparecer temporariamente como `unhealthy` antes de estabilizar.

### 2. Iniciar a aplicação no banco `transaction`

Comando usado nesta validação. O suporte automático a Docker Compose fica desligado e a conexão aponta para o MySQL já em execução.

**Windows (PowerShell):**

```powershell
.\gradlew.bat bootRun "--args=--spring.docker.compose.enabled=false --spring.datasource.url=jdbc:mysql://localhost:3307/transaction --spring.datasource.username=<USUARIO_APP_DO_COMPOSE> --spring.datasource.password=<SENHA_APP_DO_COMPOSE>"
```

**Linux / macOS:**

```bash
./gradlew bootRun --args='--spring.docker.compose.enabled=false --spring.datasource.url=jdbc:mysql://localhost:3307/transaction --spring.datasource.username=<USUARIO_APP_DO_COMPOSE> --spring.datasource.password=<SENHA_APP_DO_COMPOSE>'
```

- Substitua os marcadores pelos valores de `MYSQL_USER` e `MYSQL_PASSWORD` do seu `compose.yml` local (veja [Segurança](#segurança)).
- Opcional: `-g <pasta>` faz o Gradle usar outro diretório de cache. Nesta validação foi usado para manter o cache fora do disco do sistema.
- Alternativa do projeto base: `./gradlew bootRun` sem argumentos deixa o Spring Boot iniciar o `compose.yml` e ler as credenciais sozinho, mas exige o comando `docker` no PATH. Esse caminho **não foi validado** nesta sessão.

### 3. Usar a interface

Abra **http://localhost:8080/**. Para cadastrar uma despesa:

1. Informe a **descrição** (ex.: `Farmácia - vitaminas`).
2. Escolha a **categoria** (Mercado, Farmácia ou Automóvel).
3. Digite o **valor em reais**, com vírgula para os centavos: `80,00`, `80,5` ou `1.234,56`. Abaixo do campo aparece quantos centavos serão enviados (ex.: "R$ 80,00 será enviado à API como 8000 centavos").
4. Clique em **Adicionar despesa**. A tabela e os cards são atualizados.

O formulário rejeita descrição vazia, categoria não selecionada, valor zero ou negativo e formatos ambíguos como `80.50`.

---

## Como executar a demonstração (perfil `demo`)

O perfil `demo` usa um banco **separado**, `transaction_demo`, para não misturar dados de demonstração com os reais.

### 1. Criar o banco de demonstração (uma única vez)

```bash
docker exec -it dio-spring-ai-budgeting-database-1 mysql -uroot -p
```

Digite a senha de root quando solicitado (ela está no seu `compose.yml` local). Depois, no console MySQL:

```sql
CREATE DATABASE transaction_demo;
GRANT ALL PRIVILEGES ON transaction_demo.* TO 'app'@'%';
FLUSH PRIVILEGES;
```

As tabelas são criadas pela própria aplicação (`spring.jpa.hibernate.ddl-auto=update`). Não crie tabelas nem dados manualmente.

### 2. Iniciar com o perfil `demo`

```powershell
.\gradlew.bat bootRun "--args=--spring.profiles.active=demo --spring.docker.compose.enabled=false --spring.datasource.url=jdbc:mysql://localhost:3307/transaction_demo --spring.datasource.username=<USUARIO_APP_DO_COMPOSE> --spring.datasource.password=<SENHA_APP_DO_COMPOSE>"
```

Regras do seed (`DemoDataSeeder`):

- só existe com `--spring.profiles.active=demo`. **No modo padrão, nada é populado**;
- só popula se o banco estiver **vazio**. Se já houver qualquer transação, registra no log que nada foi criado;
- nunca apaga, substitui nem duplica dados;
- cria 12 despesas nas 3 categorias reais, passando pelo `PersistTransactionUseCase` (o mesmo caminho do REST).

---

## Interface web

Servida pelo Spring Boot a partir de `src/main/resources/static` (sem porta nem serviço adicional).

- **Cards:** total geral, Farmácia, Mercado e quantidade de lançamentos.
- **Formulário:** descrição, categoria e valor em reais. Mostra mensagens de sucesso e erro.
- **Tabela:** ordenada por valor, com filtro (Todas, Mercado, Farmácia, Automóvel), estado vazio e total da seleção.
- **Responsiva:** em telas estreitas, as linhas da tabela viram cartões. Tema claro ou escuro conforme o sistema.
- **Card "Assistente por voz":** apenas informativo e **desativado** (veja abaixo).

Como funciona:

- Usa somente os endpoints REST existentes. Como não há `GET /transactions`, a lista completa é montada com três chamadas: `GET /transactions/GROCERIES`, `/PHARMA` e `/AUTO`.
- As categorias do `<select>` repetem no JavaScript os valores reais do enum `Category`, com rótulos em português. Se o enum mudar, `js/app.js` precisa ser atualizado.
- **Dinheiro sem ponto flutuante** (`js/money.js`):
  - o texto digitado ("1.234,56") é convertido para centavos com `BigInt`;
  - o valor em reais vindo da API é lido pela sua representação decimal exata;
  - somas e formatação em BRL são feitas em centavos inteiros.

---

## API REST

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/transactions` | Cria uma despesa (JSON, `amount` em **centavos**) |
| `GET` | `/transactions/{category}` | Lista as despesas de uma categoria (`GROCERIES`, `PHARMA`, `AUTO`), `amount` em reais |
| `POST` | `/transactions/ai` | Ponto de integração de voz: recebe `multipart/form-data` com o campo `file` e devolve `audio.mp3`. **Requer `OPENAI_API_KEY`** |

> No PowerShell, use `curl.exe` em vez de `curl`.

**Criar uma despesa de R$ 80,00:**

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Farmácia", "category": "PHARMA", "amount": 8000}'
```

Resposta `201 Created`, com o valor em reais:

```json
{ "id": "…", "category": "PHARMA", "description": "Farmácia", "amount": 80.00 }
```

**Listar as despesas de farmácia:**

```bash
curl http://localhost:8080/transactions/PHARMA
```

---

## Uso da IA por voz: arquitetura e estado atual

### Ponto de integração

O back-end já tem o endpoint **`POST /transactions/ai`**. Ele recebe um áudio no campo multipart `file` e devolve um MP3. Fluxo esperado:

```
áudio (.m4a/.mp3/.wav)
   │
   ▼
POST /transactions/ai  (campo multipart "file")
   │
   ├─1. Transcrição ............ OpenAI whisper-1 (TranscriptionModel)
   ├─2. Interpretação .......... gpt-4o-mini via ChatClient + system prompt
   ├─3. Tool Calling ........... persist-transaction, list-transactions-by-category ou summarize-expenses
   │                             └─ Spring AI executa o método Java correspondente
   ├─4. Caso de uso ............ regra de negócio + acesso ao banco (MySQL)
   ├─5. Resposta em texto ...... o modelo redige a resposta com os dados da tool
   └─6. Texto para fala ........ gpt-4o-mini-tts (TextToSpeechModel)
   │
   ▼
resposta.mp3
```

A IA **apenas interpreta a intenção**. Regras de negócio, cálculos e acesso a dados ficam em código Java, expostos ao modelo somente pelas tools registradas:

| Tool | Caso de uso | Para quê |
|---|---|---|
| `persist-transaction` | `PersistTransactionUseCase` | Registrar uma despesa |
| `list-transactions-by-category` | `ListTransactionsByCategoryUseCase` | Listar as despesas de uma categoria |
| `summarize-expenses` ✨ | `SummarizeExpensesUseCase` | Total gasto, quantidade de transações e maior despesa |

A chave fica **somente no servidor**, na variável de ambiente `OPENAI_API_KEY`. A interface web nunca recebe nem envia a chave.

### Estado atual

- **Não executados nesta validação:** transcrição, interpretação por IA, Tool Calling real, geração de MP3 e os testes `*IT`. Não havia chave da OpenAI com créditos de API, e **nenhuma chamada à OpenAI foi feita**.
- **A interface ainda não tem controles de voz:** não há gravação, upload de áudio nem player de MP3. O card "Assistente por voz" é informativo e fica desativado.
- **Validado sem IA:** a lógica da tool `summarize-expenses` e a conversão de centavos, por testes unitários.

### Como validar a voz quando houver uma chave com créditos

A validação é feita **diretamente no endpoint**, por curl ou outro cliente HTTP. Não é feita pela interface.

1. Inicie a aplicação com `OPENAI_API_KEY` definida no mesmo terminal (veja [Segurança](#segurança)).
2. Envie um áudio. O repositório inclui gravações de registro de gastos em `src/test/resources/audio/`. O `recording-1.m4a` contém um comando com "80 reais", conforme o `OpenAiTranscriptionModelIT`.

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  -o resposta.mp3
```

3. Ouça o `resposta.mp3` e confira pela interface ou por `GET /transactions/{category}` se a transação foi criada.
4. Para testar a `summarize-expenses`, grave um áudio próprio (por exemplo, "Quanto gastei com farmácia?") e envie da mesma forma. Esse tipo de consulta não deve criar transações.

### Comportamento esperado (não validado)

As respostas abaixo são **ilustrativas**. O texto real é gerado pelo modelo. Os valores consideram o banco `transaction` validado: Farmácia R$ 80,00, Mercado R$ 45,00 e Combustível R$ 50,00.

| Você fala | Tool esperada | Exemplo de resposta |
|---|---|---|
| "Gastei 80 reais na farmácia" | `persist-transaction` (`amount=8000`, `PHARMA`) | "Pronto, registrei 80 reais em farmácia." |
| "Quanto gastei no total?" | `summarize-expenses` (sem categoria) | "Você gastou 175 reais em 3 compras." |
| "Quanto gastei com farmácia?" | `summarize-expenses` (`PHARMA`) | "Com farmácia, você gastou 80 reais em uma compra." |
| "Qual foi minha maior despesa?" | `summarize-expenses` (sem categoria) | "Sua maior despesa foi na farmácia, 80 reais." |
| "Liste meus gastos de mercado" | `list-transactions-by-category` (`GROCERIES`) | "No mercado, você tem uma compra de 45 reais." |

### Melhorias de IA já implementadas no código

- **Tool `summarize-expenses`:** recebe `Category` opcional e calcula em Java o total, a quantidade e a maior despesa. Sem transações, devolve `total = 0.00`, `transactionCount = 0` e `largestExpense = null`, para que o modelo não precise inventar valores.
- **Correção de centavos na saída:** `amount` continua `long` em centavos no domínio, no JPA, no banco e na entrada REST. As saídas usam `BigDecimal` com 2 casas (`BigDecimal.valueOf(amount, 2)`).
- **System prompt revisado** (`prompts/system-message.st`): respostas curtas em pt-BR, uso obrigatório da `summarize-expenses` para totais, categorias em português e proibição de inventar dados.

---

## Testes

### Testes unitários (sem infraestrutura)

Não sobem Spring e não usam MySQL, Docker nem OpenAI. Usam um repositório falso em memória (`InMemoryTransactionRepository`, só em `src/test`).

```bash
./gradlew test --tests "dio.budgeting.application.*" --tests "dio.budgeting.infrastructure.demo.*"
```

| Classe | Testes | O que valida |
|---|---|---|
| `TransactionOutputTest` | 5 | Conversão de centavos para reais (`8000 → 80.00`, `5 → 0.05`) e mapeamento dos campos |
| `SummarizeExpensesUseCaseTest` | 6 | Resumo vazio, total geral, filtro por categoria, categoria sem gastos, precisão de centavos e integração com `PersistTransactionUseCase` |
| `DemoDataSeederTest` | 3 | Seed em banco vazio, preservação de dados existentes e não duplicação em execução repetida |

### Testes que dependem de serviços externos

- `*IT` (`OpenAiChatClientIT`, `ToolCallingIT`, `OpenAiTranscriptionModelIT`…): chamam a OpenAI de verdade, só rodam com `OPENAI_API_KEY` definida e **geram custo**.
- `BudgetingApplicationTests.contextLoads`: sobe o contexto Spring completo e precisa de banco configurado.

Não foram executados nesta validação. Para rodar a suíte completa (`./gradlew test`), deixe o MySQL acessível e a chave definida.

---

## Evidências de validação

Validação local realizada com Docker Desktop, MySQL e Spring Boot.

**Testes unitários:** 14 testes, 0 falhas (`TransactionOutputTest` 5, `SummarizeExpensesUseCaseTest` 6, `DemoDataSeederTest` 3).

**Infraestrutura:** container MySQL `healthy`, porta `localhost:3307`. A aplicação subiu **sem** `OPENAI_API_KEY`.

**Banco `transaction` (dados reais):**

| Descrição | Categoria | Enviado | Retornado | Origem |
|---|---|---|---|---|
| Farmácia | PHARMA | `8000` | `80.00` | `POST /transactions` (REST) |
| Mercado | GROCERIES | `4500` | `45.00` | `POST /transactions` (REST) |
| Combustível - teste da interface | AUTO | `5000` (digitado `50,00`) | `50.00` | Formulário da interface |

- Antes do formulário: `GET /transactions/AUTO` → `[]` e a interface mostrou "Nenhuma despesa em Automóvel".
- Envio pelo formulário: **um único** `POST /transactions` → `201`, registrado pela própria página.
- Depois: `GET /transactions/AUTO` devolveu `50.00`. A interface mostrou R$ 50,00, total geral R$ 175,00 e 3 lançamentos. O banco ficou com 3 linhas (17.500 centavos).
- Persistência e UTF-8 conferidos com as descrições `Farmácia` e `Combustível`.

**Banco `transaction_demo` (perfil `demo`):**

| Categoria | Lançamentos | Total |
|---|---|---|
| Mercado | 5 | R$ 742,35 |
| Farmácia | 3 | R$ 177,79 |
| Automóvel | 4 | R$ 494,00 |
| **Total** | **12** | **R$ 1.414,14** |

- 1º startup com `demo` em banco vazio: 12 transações criadas.
- 2º startup com `demo`: o log informou que o banco já tinha dados e nada foi criado. O banco continuou com 12 linhas e 141.414 centavos.
- Interface conferida com os dados do seed:
  - cards, filtros e totais por categoria;
  - formato BRL;
  - validações do formulário, sem envio;
  - layout medido pelo DOM em 1280 px e 375 px.

**Não executado:** voz, transcrição, Tool Calling real, MP3, testes `*IT`, `contextLoads` e o `./gradlew bootRun` com suporte automático a Docker Compose.

---

## Limitações conhecidas

- **Sem edição e exclusão:** não existem endpoints `PUT` ou `DELETE`, então a interface só cadastra e consulta.
- **Sem listagem geral:** não há `GET /transactions`. A interface faz uma consulta por categoria e junta os resultados.
- **Categorias repetidas no front-end:** a API não expõe o enum, então `js/app.js` repete os três valores.
- **Voz:** o fluxo de IA exige `OPENAI_API_KEY` com créditos e ainda não tem controles na interface.
- **Sem datas:** as transações não têm data, então não há consultas por período.
- **MySQL na primeira inicialização:** o healthcheck do `compose.yml` não tem `start_period`, então o container pode aparecer `unhealthy` por alguns minutos antes de estabilizar.

---

## Segurança

- **Nunca versione `OPENAI_API_KEY`.** Não a coloque no código, no `application.properties`, no front-end nem em commits. O projeto lê `${OPENAI_API_KEY}` do ambiente do servidor.
- Para definir a chave só no terminal atual, sem exibi-la na tela nem gravá-la no histórico (PowerShell):

```powershell
$secureKey = Read-Host -AsSecureString 'OPENAI_API_KEY'
$env:OPENAI_API_KEY = [System.Net.NetworkCredential]::new('', $secureKey).Password
.\gradlew.bat bootRun
```

- No Linux/macOS, use `read -rs OPENAI_API_KEY && export OPENAI_API_KEY` antes de `./gradlew bootRun`.
- **Credenciais do banco:** os usuários e senhas do MySQL são de desenvolvimento local e estão no `compose.yml`. Consulte-os ali e não os copie para documentação, issues ou repositórios públicos. Nos comandos deste README eles aparecem como marcadores (`<USUARIO_APP_DO_COMPOSE>`, `<SENHA_APP_DO_COMPOSE>`).

---

## Decisões técnicas

- **A IA interpreta, o Java executa.** O modelo só acessa as três tools registradas no `ChatClient`. Ele não executa SQL nem operações arbitrárias.
- **Categorias como enum.** O Spring AI gera o JSON Schema da tool com a lista fechada de `Category`, então o modelo não consegue inventar categorias.
- **Dinheiro sem `double` no back-end.** Armazenamento em centavos (`long`), soma com `Math.addExact` (falha em vez de estourar em silêncio) e saída em `BigDecimal` com escala 2.
- **Dinheiro sem ponto flutuante no front-end.** Conversão de texto para centavos com `BigInt`, somas em centavos inteiros e formatação BRL manual.
- **Porta e adaptador.** O `findAll()` foi adicionado à porta `TransactionRepository` e implementado no adaptador JPA. Os casos de uso não conhecem o JPA.
- **Seed seguro.** O `DemoDataSeeder` só existe no perfil `demo`, só popula banco vazio e cria os dados pelo `PersistTransactionUseCase`.
- **Testabilidade.** Casos de uso e seed dependem só da porta, então são testados com um repositório em memória.

---

## O que aprendi

- Como funciona o ciclo de **Tool Calling** no Spring AI: `@Tool` e `@ToolParam` viram JSON Schema, o modelo pede a execução e o Spring AI chama o método Java e devolve o resultado ao modelo.
- Que o LLM deve **interpretar e redigir**, enquanto cálculos e regras de negócio ficam em código testável.
- A importância de tratar dinheiro com `long` em centavos e `BigDecimal`, evitando `double`.
- Como a arquitetura em camadas (porta de repositório + casos de uso) facilita testar sem banco e sem IA.
- Que o system prompt é parte do produto: ele define o tom, o tamanho e a confiabilidade da resposta falada.

---

## Melhorias futuras

- Interface de voz funcional (gravação ou upload de áudio e player do MP3), usando `POST /transactions/ai`, quando houver uma chave configurada
- `GET /transactions` para listar todas as despesas e um endpoint que exponha as categorias
- Edição e exclusão de despesas (`PUT`/`DELETE`)
- Data nas transações e consultas por período ("quanto gastei **este mês**?")
- Receitas e saldo estimado
- Validação no domínio (valor positivo, descrição obrigatória) e respostas de erro padronizadas (`ProblemDetail`)
- `start_period` no healthcheck do MySQL
- Endpoint de texto para testar a IA sem áudio e documentação OpenAPI/Swagger
- Extrair a orquestração de IA do controller para um serviço dedicado

---

## Referências

- [Trilha oficial DIO: Spring Boot](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html)
- [ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Audio Transcriptions API](https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html)
- [Audio Speech API](https://docs.spring.io/spring-ai/reference/api/audio/speech.html)
