# Budgeting v2: painel financeiro pessoal local com Spring Boot

Projeto do **Desafio Final do Bootcamp Itaú / DIO**: *Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot*.

> Este repositório evolui o projeto base [`05-spring-ai`](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai) da trilha oficial [dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track).

---

## Visão geral

Aplicação local de controle financeiro pessoal:

- **Receitas e despesas**, com data, meio de pagamento, recorrência (apenas como característica) e observação.
- **Lançamentos realizados e previstos**: datas futuras entram como previstas e nunca se misturam ao realizado.
- **Dashboard por período**, com receitas, despesas, saldo, comparação com o período anterior, gastos por categoria, maiores despesas, lançamentos recentes, bloco de previstos e destaques calculados localmente.
- **Interface SPA local** em HTML, CSS e JavaScript puro, servida pelo próprio Spring Boot.
- **API REST** com listagem filtrada e paginada, criação, consulta por id, edição, exclusão, dashboard e dados de referência.
- **MySQL** com schema versionado por **Flyway (V1 a V4)**, desabilitado por padrão.
- **Perfil `demo`** com seed v2 exclusivo do banco `budgeting_demo`.
- **IA por voz** (`POST /transactions/ai`): o ponto de integração existe no back-end, mas **não foi executado** (veja [IA e voz](#ia-e-voz-estado-atual)).

---

## ⚠️ Estado dos bancos

| Banco | Schema | Uso |
|---|---|---|
| `budgeting_demo` | **v2** (Flyway V1 a V4) | **Ambiente seguro para rodar a v2**, com dados de demonstração |
| `transaction` | v1 | Banco da versão anterior. **Ainda não foi migrado** |
| `transaction_demo` | v1 | Evidência da versão anterior. **Não deve ser migrado** |

- **Não habilite o Flyway apontando para `transaction` ou `transaction_demo`.** Migrar um banco v1 exige um procedimento autorizado: backup, ensaio numa cópia, comparação antes e depois, e só então aplicação no banco real.
- Com o Flyway desabilitado (padrão), a v2 **se recusa a subir** contra um banco v1 (`ddl-auto=validate` acusa colunas ausentes) e **não altera nada**. Esse comportamento foi testado numa cópia v1.

---

## Funcionalidades

| Área | O que existe |
|---|---|
| Lançamentos | Receita ou despesa, descrição, valor em centavos, categoria compatível com o tipo, data, meio de pagamento, recorrência, situação (realizado ou previsto) e observação |
| Regras | Valor > 0; descrição de 1 a 255 caracteres; data entre 01/01/2000 e hoje + 12 meses; data futura só como **previsto**; um lançamento realizado só volta a previsto por edição explícita |
| Histórico | Filtros por período, tipo, categoria e situação; paginação; totais realizado e previsto calculados no servidor |
| Edição | Edição completa, confirmação de previsto (quando a data chega) e exclusão com confirmação |
| Dashboard | Este mês, mês anterior, últimos 30 dias ou intervalo personalizado (até 366 dias) |
| Destaques locais | Maior categoria, maior gasto, comparação de despesas e **possível** cobrança recorrente (heurística transparente). Sem IA |
| Demo | Seed v2 com cerca de 40 lançamentos em 3 meses, idempotente e bloqueado fora do `budgeting_demo` |

---

## Tecnologias

- Java 25
- Spring Boot 4.0.5 (Web, Data JPA, Flyway)
- Flyway (`spring-boot-starter-flyway` e `flyway-mysql`)
- MySQL 9.6, via Docker Compose
- HTML, CSS e JavaScript puro (ES modules), sem framework, npm ou CDN
- Spring AI 2.0.0-M4 com OpenAI. Os modelos `whisper-1`, `gpt-4o-mini` e `gpt-4o-mini-tts` estão **configurados no projeto**, mas o fluxo de IA **ainda não foi executado nem validado**
- Gradle 9.4.1 (wrapper), Lombok, JUnit 5 e AssertJ

---

## Arquitetura

```
src/main/java/dio/budgeting
├── domain/                 Transaction (regras), TransactionDetails, EntryType, Category (com tipo e rótulo),
│                           PaymentMethod, Recurrence, EntryStatus, DateRange, PeriodPreset, ReportingPeriod,
│                           TransactionFilter, TransactionRepository (porta)
├── application/            casos de uso: criar (contrato completo e legado), buscar, consultar, editar,
│   │                       excluir, resumo do dashboard e resumo de despesas (tool)
│   ├── dashboard/          DashboardCalculator e InsightGenerator (cálculos determinísticos)
│   ├── input/              comandos e consultas
│   └── output/             saídas com valores em reais (BigDecimal) e em centavos
└── infrastructure/
    ├── http/               controllers, mapeamento do corpo HTTP, rotas, ProblemDetail
    ├── persistence/        entidade JPA (enums como VARCHAR) e adaptador do repositório
    ├── config/             Clock no fuso America/Sao_Paulo
    └── demo/               seed v2 (perfil demo) e seed v1 preservado (perfil demo-v1)

src/main/resources
├── db/migration/           V1__baseline … V4__financial_entry_indexes
├── static/                 index.html, css/styles.css, js/*.js (SPA por hash: #/painel e #/lancamentos)
└── prompts/                system-message.st (prompt do assistente de voz)
```

**Princípios:**
- Valores em **centavos** (`long`) no domínio e no banco, convertidos para BRL só nas bordas.
- **Cálculos determinísticos** em Java/SQL.
- A IA, quando existir, apenas consulta ferramentas e explica resultados.

### Categorias

| Tipo | Categorias |
|---|---|
| Receita | Salário, Freelance, Reembolso, Rendimentos, Outras receitas |
| Despesa | Mercado, Alimentação fora, Farmácia, Saúde, Automóvel, Moradia, Contas da casa, Assinaturas, Lazer, Educação, Outras despesas |

`GROCERIES`, `PHARMA` e `AUTO` mantêm os nomes da v1.

---

## Como executar (ambiente seguro `budgeting_demo`)

### Pré-requisitos

- JDK 25 e Docker em execução
- `OPENAI_API_KEY` **não é necessária** para a interface, a API REST e o dashboard

### 1. Preparar o `.env` (uma única vez)

As senhas do MySQL **não ficam no repositório**. O `compose.yml` lê `MYSQL_PASSWORD` e `MYSQL_ROOT_PASSWORD` de um arquivo `.env` local, que é **ignorado pelo Git e nunca deve ser versionado**. Sem ele, o Docker Compose para com a mensagem "Defina MYSQL_PASSWORD no arquivo .env".

O comando abaixo cria o `.env` a partir do `.env.example`. Ele pede as senhas com entrada oculta e não as exibe.

**Windows (PowerShell):**

```powershell
if (Test-Path .env) { Write-Host '.env já existe; nada foi alterado.' } else {
    $app  = Read-Host -AsSecureString 'MYSQL_PASSWORD (usuário da aplicação)'
    $root = Read-Host -AsSecureString 'MYSQL_ROOT_PASSWORD'
    $plain = { param($s) [System.Net.NetworkCredential]::new('', $s).Password }
    $lines = foreach ($line in Get-Content .env.example) {
        if ($line -like 'MYSQL_PASSWORD=*') { "MYSQL_PASSWORD='" + (& $plain $app) + "'" }
        elseif ($line -like 'MYSQL_ROOT_PASSWORD=*') { "MYSQL_ROOT_PASSWORD='" + (& $plain $root) + "'" }
        else { $line }
    }
    [IO.File]::WriteAllLines((Join-Path $PWD '.env'), $lines, (New-Object Text.UTF8Encoding $false))
    Write-Host '.env criado; as senhas não foram exibidas.'
}
```

**Linux / macOS:**

```bash
if [ -f .env ]; then echo ".env já existe; nada foi alterado."; else
  read -rsp 'MYSQL_PASSWORD (usuário da aplicação): ' APP_PW; echo
  read -rsp 'MYSQL_ROOT_PASSWORD: ' ROOT_PW; echo
  umask 077
  { grep -v -E '^(MYSQL_PASSWORD|MYSQL_ROOT_PASSWORD)=' .env.example
    printf "MYSQL_PASSWORD='%s'\nMYSQL_ROOT_PASSWORD='%s'\n" "$APP_PW" "$ROOT_PW"; } > .env
  unset APP_PW ROOT_PW; echo ".env criado; as senhas não foram exibidas."
fi
```

- **Use senhas próprias.** Credenciais de desenvolvimento **não devem ser reutilizadas** em nenhum outro lugar, e senhas que já apareceram em algum histórico público não devem ser usadas.
- As variáveis `MYSQL_*` só valem na **primeira inicialização** de um volume vazio. Se você já tem um volume do MySQL, o `.env` precisa ter as senhas que esse volume já usa.
- Para validar o compose, use só `docker compose config --quiet`. **Sem o `--quiet`, o comando imprime os valores resolvidos, inclusive as senhas.**

### 2. Subir o MySQL

```bash
docker compose up -d
docker compose ps
```

Espere o serviço `database` ficar `healthy`. Na primeira inicialização isso pode levar alguns minutos. O MySQL aceita conexões só da própria máquina, em `127.0.0.1:3307`; a aplicação usa `localhost:3307`. O healthcheck usa `mysqladmin ping`, sem usuário nem senha.

### 3. Criar o banco `budgeting_demo` (uma única vez)

```bash
docker exec -it dio-spring-ai-budgeting-database-1 mysql -uroot -p
```

Digite a senha de root quando solicitado (o valor de `MYSQL_ROOT_PASSWORD` no seu `.env`). No console MySQL:

```sql
CREATE DATABASE budgeting_demo;
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON budgeting_demo.* TO '<MYSQL_USER>'@'%';
```

Troque `<MYSQL_USER>` pelo valor de `MYSQL_USER` no seu `.env`. As tabelas são criadas pelo Flyway no primeiro startup; não crie tabelas nem dados manualmente.

### 4. Iniciar a aplicação em `localhost:8081`, com perfil `demo` e Flyway habilitado

As credenciais do banco são passadas por variáveis de ambiente e nunca ficam no comando. Use os valores de `MYSQL_USER` e `MYSQL_PASSWORD` do seu `.env`; a senha é lida com entrada oculta.

**Windows (PowerShell):**

```powershell
$env:SPRING_DATASOURCE_USERNAME = '<MYSQL_USER>'
$senha = Read-Host -AsSecureString 'Senha do usuário da aplicação'
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', $senha).Password
.\gradlew.bat bootRun "--args=--spring.profiles.active=demo --spring.flyway.enabled=true --spring.docker.compose.enabled=false --spring.datasource.url=jdbc:mysql://localhost:3307/budgeting_demo --server.port=8081"
```

**Linux / macOS:**

```bash
export SPRING_DATASOURCE_USERNAME='<MYSQL_USER>'
read -rs SPRING_DATASOURCE_PASSWORD && export SPRING_DATASOURCE_PASSWORD
./gradlew bootRun --args='--spring.profiles.active=demo --spring.flyway.enabled=true --spring.docker.compose.enabled=false --spring.datasource.url=jdbc:mysql://localhost:3307/budgeting_demo --server.port=8081'
```

O que acontece no primeiro startup:
1. O Flyway aplica V1 a V4 no `budgeting_demo` vazio.
2. O Hibernate valida o schema.
3. O seed v2 cria os lançamentos de demonstração.

Nos startups seguintes, o seed registra no log que o banco já tem dados e não cria nada.

Abra **http://localhost:8081/**.

> O Flyway emite um aviso de que o MySQL 9.6 é mais novo que a última versão verificada por ele (8.1). Nos ensaios, as migrações funcionaram normalmente.

---

## Interface

SPA local com navegação por hash. Rótulos em pt-BR, layout responsivo e tema claro ou escuro conforme o sistema.

- **Painel (`#/painel`):**
  - filtro de período, sempre com as datas do período e da comparação;
  - cards de receitas, despesas, saldo e quantidade de lançamentos realizados, com variação em relação ao período anterior;
  - gastos por categoria em barras de CSS;
  - 5 maiores despesas e 10 últimos lançamentos realizados;
  - destaques locais;
  - bloco **Previsto no período**, com receitas e despesas previstas, **saldo projetado** (realizado + previstos) e aviso de previstos vencidos;
  - estado vazio com botão para o primeiro lançamento.
- **Lançamentos (`#/lancamentos`):**
  - filtros e totais do servidor (realizado e previsto separados);
  - tabela no desktop e cartões no celular;
  - botão "Carregar mais";
  - previstos destacados;
  - ações Editar, Excluir e **Marcar como realizado** (só para previstos cuja data já chegou).
- **Novo e editar:**
  - `<dialog>` nativo, em tela cheia no celular;
  - campos de tipo, descrição, valor em R$, data, categoria (de acordo com o tipo), pagamento, recorrência, situação e observação;
  - o valor digitado (ex.: `1.234,56`) vira centavos com `BigInt`, sem ponto flutuante;
  - uma data futura trava a situação em **Previsto** e avisa o usuário;
  - erros da API aparecem junto a cada campo.
- **Excluir e confirmar:** diálogos mostram descrição, valor, data e categoria. A requisição só sai depois da confirmação explícita.
- **Orçamentos, Metas, Assistente e Voz:** aparecem como "em breve" ou "indisponível", conforme `features` do servidor (hoje `false`), sem controles falsos.

---

## Flyway e migrações

| Versão | Conteúdo |
|---|---|
| V1 | Schema v1 idêntico ao original (inclusive o ENUM nativo de `category`), aplicado só em banco vazio |
| V2 | Colunas `type`, `payment_method`, `recurrence`, `status`, `notes`, `occurred_at`, `created_at` e `updated_at`, com backfill dos registros v1 |
| V3 | `category` passa a `VARCHAR(40)`; campos obrigatórios viram `NOT NULL`; remove os DEFAULTs provisórios; `CHECK (amount > 0)` |
| V4 | Índices em `occurred_at`, `(type, occurred_at)` e `(category, occurred_at)` |

- **Desabilitado por padrão** (`spring.flyway.enabled=false`). Só roda com `--spring.flyway.enabled=true`.
- Bancos v1 existentes entram por **baseline** na versão 1. O `clean` fica desabilitado.
- **Backfill aplicado a registros v1:**
  - tipo despesa, meio de pagamento "outro", sem recorrência, situação realizado e sem observação;
  - `occurred_at = 2026-10-12`. Essa é a **data de backfill da migração**, não a data real em que o gasto ocorreu.
- O Flyway gratuito não desfaz migrações. A volta é feita restaurando um backup, e esse procedimento foi ensaiado.

---

## API REST

Valores entram em **centavos** (inteiro positivo) e saem em reais (`amount`, `BigDecimal`) e em centavos (`amountInCents`). Datas usam o formato `AAAA-MM-DD`. Erros vêm em `ProblemDetail`.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/transactions?start&end&type&category&status&page&size` | Histórico filtrado e paginado (`size` até 100), com `totals.confirmed` e `totals.planned` |
| `POST` | `/transactions` | Cria um lançamento (contrato completo ou legado, veja abaixo) |
| `GET` | `/transactions/{id}` | Consulta por id (só UUID completo) |
| `PUT` | `/transactions/{id}` | Edição completa; `status` obrigatório. Também serve para confirmar um previsto |
| `DELETE` | `/transactions/{id}` | Exclusão (204) |
| `GET` | `/transactions/{category}` | Rota da v1, mantida. Aceita só categorias reais do enum |
| `GET` | `/dashboard/summary?period=THIS_MONTH\|LAST_MONTH\|LAST_30_DAYS` ou `?start&end` | Resumo do período (até 366 dias). Sem parâmetros, usa o mês atual |
| `GET` | `/reference-data` | Tipos, categorias (com tipo, rótulo e descrição), meios de pagamento, recorrências, situações e `features` |
| `POST` | `/transactions/ai` | Ponto de integração de voz (multipart `file`). **Requer `OPENAI_API_KEY`; não validado** |

Um id que não é UUID completo, ou uma categoria inexistente na rota, responde **404 "Recurso não encontrado"**. Um UUID inexistente responde **404 "Lançamento não encontrado"**.

### Criar lançamento (contrato completo)

```bash
curl -X POST http://localhost:8081/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":"EXPENSE","description":"Mercado da semana","amount":15890,"category":"GROCERIES","occurredAt":"2026-10-12","paymentMethod":"DEBIT_CARD","recurrence":"NONE","notes":null}'
```

- `status` é opcional na criação: datas futuras viram `PLANNED` e as demais, `CONFIRMED`.
- Data futura com `CONFIRMED` responde 400.
- Se qualquer campo novo for enviado, `type`, `occurredAt`, `paymentMethod` e `recurrence` passam a ser obrigatórios.

### Compatibilidade temporária com o POST da v1

O corpo antigo `{"description": "...", "category": "PHARMA", "amount": 8000}`, **sem nenhum campo novo**, continua aceito:
- valores padrão: `type=EXPENSE`, `occurredAt=hoje` (America/Sao_Paulo), `paymentMethod=OTHER`, `recurrence=NONE`, `status=CONFIRMED`;
- a resposta traz o cabeçalho `X-Budgeting-Legacy-Defaults: type,occurredAt,paymentMethod,recurrence,status`;
- o servidor registra um aviso no log, sem nenhum dado financeiro;
- categoria de receita, valor decimal, zero ou negativo continuam respondendo 400.

Esse modo será **removido** quando não houver mais clientes legados. A interface v2 sempre envia o contrato completo.

### Erros

```json
{ "title": "Dados inválidos", "status": 400, "detail": "Revise os campos indicados.",
  "errors": [ { "field": "category", "message": "A categoria Salário não é compatível com despesa." } ] }
```

---

## Dashboard

- **Realizado** considera só `CONFIRMED`; **previstos** ficam num bloco separado.
- **Saldo projetado** = saldo realizado + receitas previstas − despesas previstas.
- **Comparação:**
  - "Este mês" e "Mês anterior" comparam meses completos;
  - "Últimos 30 dias" e o intervalo personalizado comparam com o período imediatamente anterior de mesma duração;
  - `deltaPercent` é `null` quando o período anterior é zero.
- Participações e variações usam `BigDecimal` com 1 casa decimal (`HALF_EVEN`); somas usam aritmética exata em centavos.
- **Destaques:** texto montado no servidor, sem IA e só com lançamentos realizados.
  - **"Possível cobrança recorrente"** é uma heurística: mesma categoria, descrição normalizada igual, valor dentro de ±10% e intervalo de 25 a 35 dias.
  - Sempre é apresentada como "possível", nunca como fato.

---

## Seed de demonstração (v2)

- Ativado só com `--spring.profiles.active=demo`. No modo padrão, nada é populado.
- Só insere dados se o banco conectado for **exatamente** o de `app.demo.database` (`budgeting_demo`) **e** estiver vazio. Em qualquer outro banco, registra "seed bloqueado" e não insere nada.
- Cria **42 lançamentos fictícios**:
  - 3 meses de datas relativas ao relógio da aplicação, mais um previsto no mês seguinte;
  - receitas (salário, freelance, reembolso, rendimentos) e despesas em várias categorias;
  - recorrência marcada só como metadado em salário, aluguel, internet e streaming;
  - previstos futuros e um previsto vencido.
- Idempotente: reiniciar não duplica nada.
- O seed v1 continua no código, no perfil `demo-v1`.

---

## IA e voz: estado atual

- **`POST /transactions/ai`** (multipart `file`) foi projetado para o fluxo áudio → transcrição → interpretação → tool (`persist-transaction`, `list-transactions-by-category` ou `summarize-expenses`) → texto → MP3.
- **Nada disso foi executado.** Não havia `OPENAI_API_KEY` com créditos, e nenhuma chamada à OpenAI foi feita.
- A tool `summarize-expenses` considera só despesas realizadas. A lógica foi validada por testes unitários.
- **Pendências conhecidas da voz:** com a v2, as tools passaram a enxergar 16 categorias, e o prompt (`system-message.st`) ainda cita só 3. Além disso, a escrita por voz deverá exigir prévia e confirmação antes de gravar.
- **Assistente textual:** não implementado (`features.assistant=false`). Quando existir, será somente leitura, com tools estreitas e sem SQL livre.
- A chave fica só no ambiente do servidor; a interface nunca a recebe.

---

## Testes

**89 testes unitários**, sem Spring, banco, Docker nem OpenAI:

```bash
./gradlew test --tests "dio.budgeting.domain.*" --tests "dio.budgeting.application.*" --tests "dio.budgeting.infrastructure.*"
```

| Área | Classes |
|---|---|
| Domínio | `TransactionTest`, `CategoryTest`, `ReportingPeriodTest` |
| Casos de uso | `PersistTransactionUseCaseTest` (incluindo o legado), `UpdateDeleteTransactionUseCaseTest`, `SearchTransactionsUseCaseTest`, `GetDashboardSummaryUseCaseTest`, `SummarizeExpensesUseCaseTest` |
| Dashboard | `DashboardCalculatorTest` (totais, comparação, previstos, destaques e heurística) |
| HTTP | `TransactionRequestMapperTest`, `RoutePatternsTest`, `ApiExceptionHandlerTest` |
| Saída e seed | `TransactionOutputTest`, `DemoDataSeederTest`, `DemoDataSeederV2Test` |

Os testes `*IT` (que chamam a OpenAI) e o `contextLoads` (contexto completo) **não foram executados**.

---

## Validações executadas

Todas em **bancos de ensaio** criados para isso. Os bancos `transaction` e `transaction_demo` não foram alterados.

- **Migração a partir do zero:** V1 a V4 aplicadas num banco vazio, com colunas, constraints e índices conferidos e `ddl-auto=validate` aprovando o schema.
- **Migração de cópias dos dados v1** (baseline + V2 a V4): contagem, soma por categoria e impressão digital das linhas **idênticas** antes e depois; backfill conferido em todas as linhas.
- **Restauração:** uma cópia migrada foi restaurada a partir do backup e voltou exatamente ao estado v1.
- **Proteção:** com o Flyway desabilitado, a v2 recusou subir contra uma cópia v1, sem alterá-la.
- **REST (35 verificações):**
  - compatibilidade com a v1;
  - contrato completo;
  - respostas 400 por campo (valor decimal, zero ou negativo; categoria incompatível; datas fora da faixa; data futura confirmada; contrato incompleto; enum e JSON inválidos);
  - consulta por id, edição, exclusão e 404;
  - parâmetros inválidos e dashboard.
- **Seed v2:** 42 lançamentos no `budgeting_demo`; reinício sem duplicar; bloqueio confirmado num banco que não é o de demonstração.
- **Interface:**
  - presets e intervalo personalizado;
  - cards, previstos e destaques;
  - filtros, paginação e totais;
  - criação, edição, confirmação e exclusão por ações reais no navegador, com erro do servidor exibido no campo;
  - celular (375 px) sem rolagem horizontal.

---

## Limitações conhecidas

- **Testcontainers não foi usado.** As queries do repositório foram validadas manualmente nos bancos de ensaio, sem teste automatizado de integração.
- **IA textual e voz não foram executadas** e exigem `OPENAI_API_KEY` com créditos. A interface não tem controles de voz.
- **O banco `transaction` ainda não foi migrado.** Para ele, a v2 só funciona depois de um procedimento de migração autorizado.
- O Flyway não declara suporte verificado ao MySQL 9.6 (apenas aviso).
- A recorrência é só uma característica: nenhum lançamento é gerado automaticamente.
- Ainda não existem orçamentos, metas, contas, cartões, tags, anexos, importação CSV/OFX nem gráficos de evolução mensal.

---

## Segurança

- **Nunca versione `OPENAI_API_KEY`** nem credenciais. O projeto lê a chave do ambiente do servidor.
- **As senhas do MySQL ficam só no `.env` local**, que é ignorado pelo Git e **nunca deve ser versionado**. O repositório traz apenas o `.env.example`, sem senhas. O `compose.yml` não tem senhas literais, inclusive no healthcheck.
- Credenciais de desenvolvimento **não devem ser reutilizadas** em outros ambientes. Nos comandos deste README elas aparecem só como marcadores (`<MYSQL_USER>`) ou são lidas com entrada oculta.
- O MySQL do Docker Compose escuta apenas em `127.0.0.1:3307`, sem exposição para a rede local.
- Backups e dumps de banco ficam fora do Git (em `build/`, que é ignorado). Nunca devem ser publicados.
- A interface usa só a API da mesma origem e insere textos com `textContent`, nunca HTML vindo da API.

---

## Decisões técnicas

- **Entidade única com tipo.** `Transaction` evoluiu com `type` (receita ou despesa), e o saldo vem do tipo, nunca do sinal do valor.
- **Dinheiro sem ponto flutuante.** Centavos em `long`, somas com `Math.addExact`, saídas em `BigDecimal`; no front-end, `BigInt`.
- **Previsto separado do realizado.** O campo `status` é explícito, então um previsto nunca vira realizado sem uma edição do usuário.
- **Schema versionado** com Flyway, desabilitado por padrão e com a validação do Hibernate como proteção.
- **Rotas sem ambiguidade.** `{id}` só casa com UUID; `{category}` só casa com valores do enum, e um teste garante que a lista acompanha o enum.
- **Erros em `ProblemDetail`** com a lista `errors` por campo.
- **Seed seguro.** Depende do perfil, de um banco explícito e de o banco estar vazio, e passa pelo mesmo caso de uso da API.

---

## O que aprendi

- Como funciona o ciclo de **Tool Calling** no Spring AI: `@Tool` e `@ToolParam` viram JSON Schema, o modelo pede a execução e o Spring AI chama o método Java e devolve o resultado ao modelo.
- Que o LLM deve **interpretar e redigir**, enquanto cálculos e regras de negócio ficam em código testável.
- A importância de tratar dinheiro com `long` em centavos e `BigDecimal`, evitando `double`.
- Como a arquitetura em camadas (porta de repositório + casos de uso) facilita testar sem banco e sem IA.
- Que o system prompt é parte do produto: ele define o tom, o tamanho e a confiabilidade da resposta falada.

---

## Melhorias futuras

- Procedimento autorizado de migração do banco `transaction`, seguido do Flyway habilitado por padrão
- Remoção do contrato legado do `POST /transactions`
- Orçamentos por categoria e alertas de gastos acima do planejado
- Metas financeiras com acompanhamento de progresso
- Recorrências e previsões, com geração de lançamentos sempre com prévia e confirmação
- Assistente textual somente leitura, com evidências dos dados usados
- Voz na interface (upload, gravação e player), com prévia e confirmação antes de qualquer escrita
- Testes de integração com Testcontainers
- Contas e cartões, tags, anexos, importação CSV/OFX e gráficos de evolução mensal

---

## Referências

- [Trilha oficial DIO: Spring Boot](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html)
- [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Flyway](https://documentation.red-gate.com/flyway)
- [Problem Details (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457)
