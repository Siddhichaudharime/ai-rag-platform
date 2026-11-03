# 🤖 AI RAG Platform

### Intelligent Document Question Answering using Retrieval-Augmented Generation (RAG)

An AI-powered document question-answering application that allows users to interact with their documents using natural language. Built with Spring Boot, React, Ollama, PostgreSQL and PGVector.

The project uses Retrieval-Augmented Generation (RAG) to retrieve relevant document information and provide context-aware AI responses using locally hosted language models.

## 🚀 Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming |
| Spring Boot | Backend framework |
| Spring AI | AI integration |
| React | Frontend development |
| TypeScript | Frontend programming |
| Ollama | Local AI model execution |
| Qwen 3 (4B) | Chat language model |
| Nomic Embed Text | Text embeddings |
| PostgreSQL | Relational database |
| PGVector | Vector storage and similarity search |
| Docker | Database containerization |
| Maven | Backend dependency management |

## ✨ Key Features

- **AI-Powered Question Answering:** Ask questions about uploaded documents using natural language.
- **Retrieval-Augmented Generation:** Retrieve relevant document context to support AI-generated answers.
- **Local AI Integration:** Run language and embedding models locally using Ollama.
- **Vector Search:** Store and retrieve document embeddings using PostgreSQL and PGVector.
- **REST API:** Backend APIs built using Spring Boot.
- **Modern Frontend:** React and TypeScript-based user interface.
- **Document Processing:** Backend document management and processing functionality.

## 🏗️ System Architecture

```text
             USER
               |
               v
       REACT + TYPESCRIPT
               |
               v
       SPRING BOOT BACKEND
               |
        +------+------+
        |             |
        v             v
  DOCUMENT        CHAT SERVICE
  PROCESSING          |
        |             |
        v             v
   EMBEDDINGS      OLLAMA
        |          QWEN 3
        v             |
     PGVECTOR         |
        |             |
        +------+------+
               |
               v
       CONTEXT-AWARE
        AI RESPONSE
               |
               v
              USER
```

## 🛠️ Project Structure

```text
ai-rag-platform/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/rishabh/rag/
│   │   │   │       ├── config/
│   │   │   │       ├── controller/
│   │   │   │       ├── dto/
│   │   │   │       ├── model/
│   │   │   │       ├── repository/
│   │   │   │       └── service/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
│
├── docker-compose.yml
├── .env.example
└── README.md
```

## ⚙️ Installation and Setup

### Prerequisites

Make sure you have installed:

- Java 21
- Node.js and npm
- Docker Desktop
- Ollama
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/Siddhichaudharime/ai-rag-platform.git

cd ai-rag-platform
```

### 2. Configure Environment Variables

Create a `.env` file in the root directory using `.env.example` as a template.

Configure your PostgreSQL credentials:

```env
DB_NAME=ragdb
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

Never commit your actual `.env` file.

### 3. Start PostgreSQL with PGVector

```bash
docker compose up -d
```

### 4. Install AI Models

Make sure Ollama is running, then execute:

```bash
ollama pull qwen3:4b
ollama pull nomic-embed-text
```

### 5. Run the Backend

Open a terminal:

```bash
cd backend
```

Set your database environment variables in PowerShell if they are not already configured:

```powershell
$env:DB_NAME="ragdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
```

Start Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend runs at:

`http://localhost:8080`

### 6. Run the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local frontend URL displayed by Vite in your terminal.

## 🎯 Project Objectives

This project explores the practical implementation of Retrieval-Augmented Generation by combining:

- Large Language Models (LLMs)
- Text embeddings
- Vector databases
- Semantic retrieval
- REST API development
- Full-stack application development

## 🔮 Future Enhancements

- Support for additional document formats.
- Improved document chunking and retrieval.
- Streaming AI responses.
- Authentication and user management.
- Improved error handling and monitoring.
- Deployment to a cloud platform.

## 👨‍💻 Developer

**Siddhichaudharime**

Aspiring Software Developer interested in Java, Spring Boot, Full-Stack Development and Generative AI.

**GitHub:** https://github.com/Siddhichaudharime

---

⭐ If you find this project interesting, consider giving it a star!

*Built as a hands-on exploration of AI-powered application development.*
