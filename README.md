# 🧠 RAG POC: Retrieval-Augmented Generation com AWS Bedrock e S3

> **Prova de conceito** de um sistema RAG (Recuperação + Geração Aumentada) usando Java, Spring Boot, AWS Bedrock e S3.

---

## 📌 Sobre o Projeto

Esta é uma **prova de conceito** de um sistema RAG que:

- Faz **upload de documentos PDF** para o **AWS S3**
- **Indexa** o conteúdo em um banco vetorial (PostgreSQL + pgvector)
- **Recupera** trechos relevantes usando busca por similaridade semântica (embeddings)
- **Gera respostas** contextualizadas usando Amazon Nova Lite

A aplicação **não alucina**: se a informação não estiver no documento, responde que não sabe.

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTE (Postman/Web)                   │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot Application                      │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │   Upload    │ →  │  Salvar no  │ →  │  Baixar do S3 e     │  │
│  │   (PDF)     │    │  S3 (bucket)│    │  indexar (embedding)│  │
│  └─────────────┘    └─────────────┘    └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │                   │                         │
         ▼                   ▼                         ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐
│   PostgreSQL    │  │   AWS S3        │  │    AWS Bedrock      │
│   + pgvector    │  │   (Documentos)  │  │    (Nova Lite +     │
│   (Vetores)     │  │                 │  │     Cohere)         │
└─────────────────┘  └─────────────────┘  └─────────────────────┘
```

### Fluxo da pergunta (RAG)

1. Usuário pergunta algo (`POST /api/chat`)
2. A pergunta é convertida em **embedding** (Cohere)
3. Busca no pgvector os **trechos mais similares**
4. Se encontrar, monta um prompt com contexto
5. LLM (Nova Lite) gera resposta **baseada apenas no documento**
6. Se não encontrar, responde: *"Não encontrei essa informação no currículo"*

---

## 🛠️ Tecnologias

| Camada | Tecnologia |
|--------|------------|
| **Linguagem** | Java 17 |
| **Framework** | Spring Boot 3.2.4 |
| **LLM (Chat)** | Amazon Nova Lite (via Bedrock) |
| **Embeddings** | Cohere Embed English v3 (via Bedrock) |
| **Armazenamento** | AWS S3 (documentos originais) |
| **Banco Vetorial** | PostgreSQL 16 + pgvector |
| **Parsing de PDF** | Apache PDFBox |
| **Build** | Maven |
| **Container** | Docker (PostgreSQL) |

---

## 📋 Pré-requisitos

- Java 17+
- Docker
- Conta AWS com acesso ao Bedrock (Nova Lite + Cohere habilitados)
- Bucket S3 criado

---

## 🔐 Configuração de Credenciais

### Localmente (desenvolvimento)

Configure as variáveis de ambiente no IntelliJ:

```
AWS_ACCESS_KEY_ID=sua_chave
AWS_SECRET_ACCESS_KEY=sua_chave_secreta
AWS_REGION=us-east-1
DB_USERNAME=postgres
DB_PASSWORD=senha123
```

### Na AWS (produção)

A aplicação utiliza **IAM Roles** quando rodando na AWS. Crie uma IAM Role com as políticas:
- `AmazonBedrockFullAccess`
- `AmazonS3FullAccess`

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

### 3. Configurar o `application.yml`

```yaml
aws:
  region: us-east-1
  s3:
    bucket: seu-bucket-name
  bedrock:
    model-id: amazon.nova-lite-v1:0
    embedding-model-id: cohere.embed-english-v3

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ragpoc
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
```

### 4. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

---

## 🔌 Endpoints

### 📄 Upload de documento (com S3)

```http
POST /api/documentos/upload
Content-Type: multipart/form-data

file: @caminho/do/seu/curriculo.pdf
```

**Resposta:**
```json
"✅ Documento indexado com sucesso!
   Arquivo: curriculo.pdf
   S3 Key: uploads/1234567890_curriculo.pdf"
```

### 💬 Perguntar (RAG)

```http
POST /api/chat
Content-Type: application/json

{
    "pergunta": "Liste todas as minhas experiências com Cloud"
}
```

**Resposta (vem do currículo):**
```json
{
    "resposta": "Experiências com Cloud:\n\n1. **AWS** - Lambda, EC2, S3, DynamoDB, Kinesis, SQS, SNS\n2. **Azure** - Evolução de sistemas para microserviços\n3. **Multi-Cloud** - Plataforma com AWS, Azure e GCP"
}
```

### 🩺 Health check

```http
GET /api/chat/health
```

**Resposta:** `"RAG POC está rodando!"`

---

## 🎬 Demonstração

**Indexando um currículo (S3 + PostgreSQL):**
```
POST /api/documentos/upload → 200 OK
"✅ Documento indexado com sucesso! S3 Key: uploads/1734889123456_curriculo.pdf"
```

**Perguntando sobre o documento:**
- Pergunta: `"Qual minha experiência com AWS?"`
- Resposta: `"AWS (Lambda, EC2, S3, DynamoDB, Kinesis, SQS, SNS)"`

**Evitando alucinação (pergunta fora do contexto):**
- Pergunta: `"Me dê uma receita de bolo de laranja"`
- Resposta: `"Não encontrei essa informação no currículo."`

---

## 📁 Estrutura de Pastas

```
src/main/java/com/home/ragpoc/
├── config/
│   ├── BedrockConfig.java          # Bedrock (Nova Lite + Cohere)
│   ├── S3Config.java               # S3 Client
│   └── VectorStoreConfig.java      # PostgreSQL + pgvector
├── controller/
│   ├── ChatController.java         # Endpoint /api/chat
│   └── DocumentController.java     # Endpoint /api/documentos/upload
├── service/
│   └── RagService.java             # RAG + Indexação
├── model/
│   └── ChatRequest.java            # DTO
└── RagPocApplication.java
```

---

## 🧠 Decisões Técnicas

| Decisão | Motivo |
|---------|--------|
| **S3 para armazenamento** | Persistência dos documentos originais, escalável e seguro |
| **pgvector para busca** | Busca por similaridade diretamente no PostgreSQL |
| **Cohere para embeddings** | Ativação automática, multilíngue e sem formulário |
| **Nova Lite para chat** | Modelo rápido, barato e com boa qualidade |
| **Segmentos de 1500 caracteres** | Mantém contexto completo das experiências |

---

## 🔮 Melhorias Futuras

- [ ] Interface web (Thymeleaf ou React)
- [ ] Listar documentos no S3 (`GET /api/documentos`)
- [ ] Deletar documentos (`DELETE /api/documentos/{s3Key}`)
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

> 💡 *Este projeto foi construído para demonstrar habilidades em Java, Spring Boot, AWS Bedrock, S3 e RAG.*s