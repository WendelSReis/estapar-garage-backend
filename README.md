# Estapar Backend Developer Test

Projeto completo em **Java 21 + Spring Boot 3.x + MySQL 8 + Maven + Testcontainers + Docker**. Este README foi ajustado com os comandos reais usados para subir o banco, o simulador e executar os testes locais.

## Solução adotada

O serviço implementa:
- sincronização inicial da garagem via `GET /garage` no startup;
- recebimento de eventos `ENTRY`, `PARKED` e `EXIT` em `POST /webhook`;
- controle de vagas por setor;
- cálculo de preço com regra de 30 minutos grátis + preço dinâmico;
- consulta de faturamento por data e setor em `GET /revenue`.

## Premissas assumidas

O desafio informa que o sistema deve buscar a configuração da garagem no simulador e aceitar eventos no webhook `http://localhost:3003/webhook`. Também pede o endpoint `GET /revenue` por data e setor. fileciteturn9file1

Como o evento `ENTRY` não traz setor, a implementação adota a seguinte estratégia:
- `ENTRY` cria a sessão ativa do veículo;
- `PARKED` identifica a vaga real por `lat/lng`, associa o setor e marca a vaga como ocupada;
- `EXIT` calcula o valor, fecha a sessão e libera a vaga.

## Stack

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Flyway
- MySQL 8
- Maven
- Testcontainers
- Docker / Docker Compose
- Git

## Requisitos para rodar

Instale:
- JDK 21
- Maven 3.9+
- Docker Desktop
- Git
- Postman

## Estrutura do projeto

```text
src
├── main
│   ├── java/com/estapar/garage
│   │   ├── api/controller
│   │   ├── api/dto
│   │   ├── bootstrap
│   │   ├── config
│   │   ├── domain/enums
│   │   ├── domain/model
│   │   ├── domain/service
│   │   ├── exception
│   │   ├── integration/simulator
│   │   └── persistence/{entity,repository}
│   └── resources
│       ├── application.yml
│       └── db/migration/V1__init.sql
└── test
    └── java/com/estapar/garage
```

## Como subir localmente

### 1. Subir o MySQL

Na raiz do projeto:

```bash
docker compose up -d mysql
```

Verificar se o container subiu:

```bash
docker ps
```

Verificar logs do MySQL:

```bash
docker logs estapar-mysql
```

O esperado é ver algo como `ready for connections`.

### 2. Subir o simulador

O enunciado do teste pede para iniciar o simulador com Docker.

No Windows/PowerShell, o comando que funcionou corretamente foi expor a porta `3000`:

```bash
docker run -d --name garage-sim -p 3000:3000 cfontes0estapar/garage-sim:1.0.0
```

Se já existir um container antigo do simulador, remova antes:

```bash
docker rm -f garage-sim
```

Se o simulador tiver sido criado com nome automático, remova pelo nome exibido no `docker ps`:

```bash
docker rm -f <nome-ou-id-do-container>
```

### 3. Validar o simulador

Teste o endpoint:

```powershell
curl http://localhost:3000/garage
```

ou:

```powershell
Invoke-WebRequest http://localhost:3000/garage
```

O esperado é `StatusCode : 200` e um JSON com `garage` e `spots`.

### 4. Rodar a aplicação

```bash
mvn clean spring-boot:run
```

### 5. Validar se a aplicação subiu

A aplicação deve subir na porta `3003`.

Teste:

```powershell
curl http://localhost:3003/revenue?date=2025-01-01\&sector=A
```

Se ainda não houver dados fechados, pode retornar `0.00`, o que é esperado no início.

## Comandos úteis de troubleshooting

### Ver containers ativos

```bash
docker ps
```

### Ver logs do MySQL

```bash
docker logs estapar-mysql
```

### Ver logs do simulador

```bash
docker logs garage-sim
```

### Derrubar containers do compose

```bash
docker compose down
```

### Subir novamente só o MySQL

```bash
docker compose up -d mysql
```

### Remover e recriar o simulador

```bash
docker rm -f garage-sim
docker run -d --name garage-sim -p 3000:3000 cfontes0estapar/garage-sim:1.0.0
```

## Como gerar o JAR

```bash
mvn clean package
```

## Como rodar os testes

### Rodar todos os testes

```bash
mvn clean test
```

### Rodar apenas teste unitário

```bash
mvn -Dtest=ParkingPricingServiceTest test
```

## Endpoints para testar no Postman

O desafio define o webhook em `http://localhost:3003/webhook` e a consulta de receita em `GET /revenue`.

Base URL local:

```text
http://localhost:3003
```

### 1. Webhook - ENTRY

**Método:** `POST`  
**URL:**

```text
http://localhost:3003/webhook
```

**Headers:**

```text
Content-Type: application/json
```

**Body (raw / JSON):**

```json
{
  "license_plate": "ZUL0001",
  "entry_time": "2025-01-01T12:00:00.000Z",
  "event_type": "ENTRY"
}
```

Exemplo do enunciado:

### 2. Webhook - PARKED

**Método:** `POST`  
**URL:**

```text
http://localhost:3003/webhook
```

**Headers:**

```text
Content-Type: application/json
```

**Body (raw / JSON):**

```json
{
  "license_plate": "ZUL0001",
  "lat": -23.561684,
  "lng": -46.655981,
  "event_type": "PARKED"
}
```

Exemplo do enunciado:

### 3. Webhook - EXIT

**Método:** `POST`  
**URL:**

```text
http://localhost:3003/webhook
```

**Headers:**

```text
Content-Type: application/json
```

**Body (raw / JSON):**

```json
{
  "license_plate": "ZUL0001",
  "exit_time": "2025-01-01T13:01:00.000Z",
  "event_type": "EXIT"
}
```

Exemplo do enunciado:

### 4. Revenue

O PDF mostra a consulta por data e setor.

**Método:** `GET`  
**URL:**

```text
http://localhost:3003/revenue?date=2025-01-01&sector=A
```

**Resposta esperada:**

```json
{
  "amount": 0.00,
  "currency": "BRL",
  "timestamp": "2025-01-01T12:00:00.000Z"
}
```

## Ordem recomendada para testar no Postman

Para o fluxo do desafio, execute nesta ordem:

1. `POST /webhook` com `ENTRY`
2. `POST /webhook` com `PARKED`
3. `POST /webhook` com `EXIT`
4. `GET /revenue?date=2025-01-01&sector=A`

## Coleção sugerida no Postman

### Pasta: Webhook

#### Request 1 - ENTRY

```json
{
  "license_plate": "ZUL0001",
  "entry_time": "2025-01-01T12:00:00.000Z",
  "event_type": "ENTRY"
}
```

#### Request 2 - PARKED

```json
{
  "license_plate": "ZUL0001",
  "lat": -23.561684,
  "lng": -46.655981,
  "event_type": "PARKED"
}
```

#### Request 3 - EXIT

```json
{
  "license_plate": "ZUL0001",
  "exit_time": "2025-01-01T13:01:00.000Z",
  "event_type": "EXIT"
}
```

### Pasta: Revenue

#### Request 4 - GET Revenue

```text
GET http://localhost:3003/revenue?date=2025-01-01&sector=A
```

## Exemplo completo usando curl

### ENTRY

```bash
curl --location 'http://localhost:3003/webhook' \
--header 'Content-Type: application/json' \
--data '{
  "license_plate": "ZUL0001",
  "entry_time": "2025-01-01T12:00:00.000Z",
  "event_type": "ENTRY"
}'
```

### PARKED

```bash
curl --location 'http://localhost:3003/webhook' \
--header 'Content-Type: application/json' \
--data '{
  "license_plate": "ZUL0001",
  "lat": -23.561684,
  "lng": -46.655981,
  "event_type": "PARKED"
}'
```

### EXIT

```bash
curl --location 'http://localhost:3003/webhook' \
--header 'Content-Type: application/json' \
--data '{
  "license_plate": "ZUL0001",
  "exit_time": "2025-01-01T13:01:00.000Z",
  "event_type": "EXIT"
}'
```

### REVENUE

```bash
curl --location 'http://localhost:3003/revenue?date=2025-01-01&sector=A'
```

## Regras implementadas

### Gratuidade
- até 30 minutos: `R$ 0,00`

### Cobrança
- acima de 30 minutos: cobra por hora com arredondamento para cima
- exemplo: 31 min = 1 hora, 61 min = 2 horas

### Preço dinâmico
Baseado na ocupação do setor:
- menor que 25% -> desconto de 10%
- até 50% -> sem ajuste
- até 75% -> acréscimo de 10%
- até 100% -> acréscimo de 25%

### Lotação
- com 100% de lotação, o setor fecha para novas entradas até a saída de um veículo. fileciteturn9file1

## Melhorias futuras

- idempotência por evento;
- observabilidade com Actuator e Micrometer;
- fila assíncrona para eventos;
- retry/circuit breaker para o simulador;
- documentação OpenAPI.
