# API de Orçamento com Comandos de Voz — Spring Boot + Spring AI

Projeto do **Desafio Final do Bootcamp Itaú / DIO**: *Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot*.

A API registra e consulta gastos a partir de **comandos de voz**. Você envia um áudio como "gastei 80 reais na farmácia" ou "quanto gastei com farmácia?" e recebe a resposta também em áudio.

> Este repositório evolui o projeto base [`05-spring-ai`](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai) da trilha oficial [dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track).

---

## O que o projeto faz

```
áudio (.m4a/.mp3/.wav)
   │
   ▼
POST /transactions/ai
   │
   ├─1. Transcrição ............ OpenAI whisper-1 (TranscriptionModel)
   ├─2. Interpretação .......... gpt-4o-mini via ChatClient + system prompt
   ├─3. Tool Calling ........... o modelo escolhe uma das tools registradas
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
| `persist-transaction` | `PersistTransactionUseCase` | Registrar um gasto |
| `list-transactions-by-category` | `ListTransactionsByCategoryUseCase` | Listar os gastos de uma categoria |
| `summarize-expenses` ✨ | `SummarizeExpensesUseCase` | Total gasto, quantidade de transações e maior despesa |

---

## Melhoria implementada

### 1. Nova tool `summarize-expenses`

Responde perguntas de resumo financeiro com **cálculos feitos em Java, nunca pelo LLM**:

- **Entrada:** `Category` opcional. Sem categoria, considera todos os gastos (`findAll()`). Com categoria, considera só aquela (`findAllByCategory(category)`).
- **Saída (`ExpenseSummaryOutput`):**

```json
{
  "category": "PHARMA",
  "total": 99.99,
  "transactionCount": 2,
  "largestExpense": { "id": "…", "description": "Farmácia", "category": "PHARMA", "value": 80.00 }
}
```

- **Sem transações:** `total = 0.00`, `transactionCount = 0` e `largestExpense = null`. Assim o modelo pode dizer "não encontrei gastos" sem inventar valores.

**Por quê?** No projeto base, para responder "quanto gastei com farmácia?", o modelo recebia a lista de transações e somava sozinho. LLMs erram aritmética. Agora a conta é feita em código testável e o modelo só redige a resposta.

### 2. Correção de centavos na saída (pré-requisito)

O valor é armazenado como `long` em **centavos** (`8000` = R$ 80,00), mas o projeto base devolvia `8000.0` como se fossem reais. Agora:

- Domínio, JPA, banco e entrada REST **continuam em centavos** (`long`).
- As saídas (`TransactionOutput`, `TransactionResponse`, `ExpenseSummaryOutput`) usam **`BigDecimal` com 2 casas**, convertidas com `BigDecimal.valueOf(amount, 2)`. A conversão é exata e não usa `double`.

### 3. System prompt revisado

O arquivo `src/main/resources/prompts/system-message.st` passou a orientar o modelo a:

- responder em pt-BR, em até duas frases curtas, sem markdown (a resposta vira áudio);
- usar **sempre** a tool `summarize-expenses` para totais, quantidades, maior despesa e resumos;
- falar as categorias em português ("farmácia" em vez de `PHARMA`);
- usar apenas dados retornados pelas tools e **nunca inventar valores**.

---

## Tecnologias

- Java 25
- Spring Boot 4.0.5 (Web, Data JPA, Docker Compose support)
- Spring AI 2.0.0-M4 com OpenAI:
  - `whisper-1` para transcrição de áudio
  - `gpt-4o-mini` para chat e Tool Calling
  - `gpt-4o-mini-tts` para texto em fala
- MySQL 9.6, iniciado via Docker Compose
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
    └── persistence/        TransactionEntity, JpaTransactionRepository (adaptador da porta)
```

Categorias disponíveis (`Category`): `GROCERIES` (mercado/alimentação), `PHARMA` (farmácia), `AUTO` (carro/combustível).

---

## Como executar

### Pré-requisitos

- JDK 25
- Docker em execução (o MySQL sobe automaticamente pelo `compose.yml` ao rodar a aplicação)
- Uma chave da OpenAI na variável de ambiente `OPENAI_API_KEY`

> ⚠️ Nunca coloque a chave no código ou no `application.properties`. O projeto lê `${OPENAI_API_KEY}` do ambiente.

### Linux / macOS

```bash
export OPENAI_API_KEY="sua_chave_aqui"
./gradlew bootRun
```

### Windows (PowerShell)

```powershell
$env:OPENAI_API_KEY = "sua_chave_aqui"   # vale só para este terminal
.\gradlew.bat bootRun
```

A API sobe em `http://localhost:8080`. O MySQL é exposto na porta `3307`.

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/transactions` | Cria um gasto (JSON, `amount` em **centavos**) |
| `GET` | `/transactions/{category}` | Lista os gastos de uma categoria (`GROCERIES`, `PHARMA`, `AUTO`) |
| `POST` | `/transactions/ai` | Recebe um áudio (`multipart/form-data`, campo `file`) e devolve `audio.mp3` |

### Exemplos de requisições REST

> No PowerShell, use `curl.exe` em vez de `curl`.

**Criar um gasto de R$ 80,00:**

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Farmácia", "category": "PHARMA", "amount": 8000}'
```

Resposta (`201 Created`), com o valor já em reais:

```json
{ "id": "…", "category": "PHARMA", "description": "Farmácia", "amount": 80.00 }
```

**Listar os gastos de farmácia:**

```bash
curl http://localhost:8080/transactions/PHARMA
```

### Exemplo de comando de voz

O projeto já traz gravações de exemplo em `src/test/resources/audio/` (comandos de registro de gastos):

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  -o resposta.mp3
```

Abra o `resposta.mp3` para ouvir a resposta. Para testar consultas, grave seu próprio áudio (por exemplo, com o Gravador de Voz do Windows ou com o celular) e envie da mesma forma.

### Comandos de voz suportados

As respostas abaixo são ilustrativas. O texto exato varia, porque é gerado pelo modelo a partir dos dados retornados pela tool.

| Você fala | Tool chamada | Exemplo de resposta |
|---|---|---|
| "Gastei 80 reais na farmácia" | `persist-transaction` (`amount=8000`, `PHARMA`) | "Pronto, registrei 80 reais em farmácia." |
| "Quanto gastei no total?" | `summarize-expenses` (sem categoria) | "Você gastou 240 reais e 50 centavos em 3 compras." |
| "Quanto gastei com farmácia?" | `summarize-expenses` (`PHARMA`) | "Com farmácia, você gastou 80 reais em uma compra." |
| "Qual foi minha maior despesa?" | `summarize-expenses` (sem categoria) | "Sua maior despesa foi no mercado, 120 reais e 50 centavos." |
| "Faça um resumo dos meus gastos" | `summarize-expenses` (sem categoria) | "Você tem 3 gastos que somam 240 reais e 50 centavos; o maior foi no mercado." |
| "Liste meus gastos de mercado" | `list-transactions-by-category` (`GROCERIES`) | "No mercado, você tem uma compra de 120 reais e 50 centavos." |

---

## Testes

### Testes unitários (sem infraestrutura)

Não sobem Spring e não usam MySQL, Docker nem OpenAI. Usam um repositório falso em memória (`InMemoryTransactionRepository`, só em `src/test`).

```bash
./gradlew test --tests "dio.budgeting.application.*"
```

| Classe | O que valida |
|---|---|
| `TransactionOutputTest` | Conversão de centavos para reais (`8000 → 80.00`, `5 → 0.05`) e mapeamento dos campos |
| `SummarizeExpensesUseCaseTest` | Resumo vazio, total geral, filtro por categoria, categoria sem gastos, precisão de centavos e integração com `PersistTransactionUseCase` |

### Testes de integração (dependem de serviços externos)

- `*IT` (`OpenAiChatClientIT`, `ToolCallingIT`, `OpenAiTranscriptionModelIT`…): chamam a OpenAI de verdade e só rodam quando `OPENAI_API_KEY` está definida, o que **gera custo**.
- `BudgetingApplicationTests.contextLoads`: sobe o contexto Spring completo e precisa de banco e chave configurados.

Para rodar a suíte completa (`./gradlew test`), deixe o MySQL acessível e a chave definida.

---

## Decisões técnicas

- **A IA interpreta, o Java executa.** O modelo só acessa as três tools registradas no `ChatClient`. Ele não executa SQL nem operações arbitrárias.
- **Categorias como enum.** O Spring AI gera o JSON Schema da tool com a lista fechada de `Category`, então o modelo não consegue inventar categorias.
- **Dinheiro sem `double`.** Armazenamento em centavos (`long`), soma com `Math.addExact` (falha em vez de estourar em silêncio) e saída em `BigDecimal` com escala 2.
- **Porta e adaptador.** O `findAll()` foi adicionado à porta `TransactionRepository` do domínio e implementado no adaptador JPA. O caso de uso não conhece o JPA.
- **Testabilidade.** Como o caso de uso depende só da porta, ele é testado com um repositório em memória, de forma rápida e determinística.

---

## O que aprendi

- Como funciona o ciclo de **Tool Calling** no Spring AI: `@Tool` e `@ToolParam` viram JSON Schema, o modelo pede a execução e o Spring AI chama o método Java e devolve o resultado ao modelo.
- Que o LLM deve **interpretar e redigir**, enquanto cálculos e regras de negócio ficam em código testável.
- A importância de tratar dinheiro com `long` em centavos e `BigDecimal`, evitando `double`.
- Como a arquitetura em camadas (porta de repositório + casos de uso) facilita testar sem banco e sem IA.
- Que o system prompt é parte do produto: ele define o tom, o tamanho e a confiabilidade da resposta falada.

---

## Melhorias futuras

- Data nas transações e consultas por período ("quanto gastei **este mês**?")
- Receitas e saldo estimado
- Validação no domínio (valor positivo, descrição obrigatória) e respostas de erro padronizadas (`ProblemDetail`)
- Endpoint de texto para testar a IA sem áudio e documentação OpenAPI/Swagger
- Extrair a orquestração de IA do controller para um serviço dedicado
- Mais categorias de gastos

---

## Referências

- [Trilha oficial DIO — Spring Boot](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html)
- [ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Audio Transcriptions API](https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html)
- [Audio Speech API](https://docs.spring.io/spring-ai/reference/api/audio/speech.html)
