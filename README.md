# 🧠 RAG POC: Retrieval-Augmented Generation com AWS Bedrock

> **Prova de conceito** de um sistema de RAG (Recuperação + Geração Aumentada) usando Java, Spring Boot e AWS Bedrock.

---

## 📌 Sobre o Projeto

Esta é uma **prova de conceito** de um sistema RAG que:

- Faz **upload de documentos PDF** (ex: currículo)
- **Indexa** o conteúdo em um banco vetorial (PostgreSQL + pgvector)
- **Recupera** trechos relevantes usando busca por similaridade semântica
- **Gera respostas** contextualizadas usando Amazon Nova Lite

A aplicação **não alucina**: se a informação não estiver no documento, responde que não sabe.

---

## 🏗️ Arquitetura

```
CLIENTE (Postman)
       │
       ▼
Spring Boot Application
  ├── Upload PDF → Indexar (Embeddings) → Buscar + Gerar (RAG)
       │                   │                         │
       ▼                   ▼                         ▼
PostgreSQL + pgvector   AWS Bedrock (Cohere)    AWS Bedrock (Nova Lite)
     (Vetores)             (Embeddings)              (Chat/LLM)
```

**Fluxo da pergunta:**
1. Usuário pergunta → 2. Gera embedding → 3. Busca similar no banco → 4. Monta contexto → 5. LLM responde baseada no documento

---

## 🛠️ Tecnologias

| Camada | Tecnologia |
|--------|------------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2.4 |
| LLM (Chat) | Amazon Nova Lite (Bedrock) |
| Embeddings | Cohere Embed English v3 (Bedrock) |
| Banco Vetorial | PostgreSQL 16 + pgvector |
| Parsing PDF | Apache PDFBox |
| Build | Maven |
| Container | Docker |

---

## 📋 Pré-requisitos

- Java 17+
- Docker
- Conta AWS com acesso ao Bedrock (Nova Lite + Cohere habilitados)

---

## 🚀 Como Rodar Localmente

### 1. Subir o PostgreSQL com pgvector

```bash
docker run --name postgres-rag \
  -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=ragpoc \
  -p 5432:5432 \
  -d pgvector/pgvector:pg16
```

### 2. Habilitar a extensão vector

```bash
docker exec -it postgres-rag psql -U postgres -d ragpoc -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 3. Configurar variáveis de ambiente

No IntelliJ: `Run` → `Edit Configurations` → `Environment variables`:

```
AWS_ACCESS_KEY_ID=sua_chave;AWS_SECRET_ACCESS_KEY=sua_chave_secreta;AWS_REGION=us-east-1;DB_USERNAME=postgres;DB_PASSWORD=senha123
```

### 4. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

---

## 🔌 Endpoints

### 📄 Upload de documento

```http
POST /api/documentos/upload
Content-Type: multipart/form-data

file: @caminho/do/seu/curriculo.pdf
```

**Resposta:** `"✅ Documento indexado com sucesso: curriculo.pdf"`

### 💬 Perguntar (RAG)

```http
POST /api/chat
Content-Type: application/json

{
    "pergunta": "Qual minha experiência com AWS?"
}
```

**Resposta (vem do currículo):**
```json
{
    "resposta": "Experiência com AWS Lambda, EC2, S3, DynamoDB, Kinesis, SQS e SNS..."
}
```

### 🩺 Health check

```http
GET /api/chat/health
```

**Resposta:** `"RAG POC está rodando!"`

---

## 🎬 Demonstração

**Indexando um currículo:**
```
POST /api/documentos/upload → 200 OK
"✅ Documento indexado com sucesso: profile-jose-arimatea-vieira-pt.pdf"
```

**Perguntando sobre o documento:**
```json
{
    "pergunta": "Qual minha experiência com AWS?"
}
```
**Resposta:** `"Experiência com AWS (Lambda, EC2, S3, DynamoDB, Kinesis, SQS, SNS)..."`

**Evitando alucinação (pergunta fora do contexto):**
```json
{
    "pergunta": "Me dê uma receita de bolo de laranja"
}
```
**Resposta:** `"Não encontrei essa informação no currículo."`

---

## 📁 Estrutura de Pastas

```
src/main/java/com/home/ragpoc/
├── config/                     # Configurações (Bedrock, VectorStore)
├── controller/                 # Endpoints REST (Chat, Upload)
├── service/                    # Lógica de negócio (RAG + Indexação)
├── model/                      # DTOs
└── RagPocApplication.java      # Entry point
```

---

## 🧠 Decisões Técnicas

| Decisão | Motivo |
|---------|--------|
| LangChain4j 0.36.2 | Versão estável com suporte a Bedrock |
| Converse API customizada | LangChain4j não suportava Nova Lite nativamente |
| Cohere para embeddings | Ativação automática, multilíngue |
| pgvector no Docker | Simplicidade para POC, sem custos |
| Variáveis de ambiente | Segurança (credenciais não vão para o GitHub) |

---

## 🔮 Melhorias Futuras

- [ ] Interface web (Thymeleaf ou React)
- [ ] Suporte a múltiplos documentos
- [ ] Cache de embeddings
- [ ] Deploy na AWS (ECS ou Lambda)
- [ ] Testes de integração com Testcontainers
- [ ] CI/CD com GitHub Actions

---

## 👨‍💻 Autor

**José Arimatea Oliveira**

- GitHub: [github.com/arimateaoliveira](https://github.com/arimateaoliveira)
- LinkedIn: [linkedin.com/in/arimateaoliveira](https://linkedin.com/in/arimateaoliveira)

---

## 📄 Licença

MIT

---

> 💡 *Este projeto foi construído para demonstrar habilidades em Java, Spring Boot, AWS Bedrock e RAG.*