package com.rishabh.rag.service;

import com.rishabh.rag.dto.ChatResponse;
import com.rishabh.rag.dto.SourceInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore
    ) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty.");
        }

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(message.trim())
                        .topK(3)
                        .similarityThreshold(0.2)
                        .build()
        );
    }

    public String buildContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);

            Object fileName = document.getMetadata().get("fileName");
            Object page = document.getMetadata().get("page_number");

            context.append("DOCUMENT CHUNK ")
                    .append(i + 1)
                    .append("\n");

            context.append("File: ")
                    .append(fileName != null ? fileName : "Unknown")
                    .append("\n");

            if (page != null) {
                context.append("Page: ")
                        .append(page)
                        .append("\n");
            }

            context.append("Content:\n")
                    .append(document.getText())
                    .append("\n\n");
        }

        return context.toString();
    }

    public Flux<String> streamAnswer(
            String question,
            String context
    ) {
        String systemPrompt = """
                You are the AI assistant for a Document RAG project.

                You can handle two types of conversation.

                TYPE 1 - NORMAL AND PROJECT CONVERSATION:
                You may answer greetings, casual conversation, and questions
                about this RAG project using the project knowledge below.

                PROJECT KNOWLEDGE:
                - This project is an AI-powered Retrieval-Augmented Generation
                  (RAG) document chatbot.
                - Users can upload PDF, DOCX, TXT and Markdown files.
                - Uploaded documents are processed and split into smaller chunks.
                - The chunks are converted into vector embeddings.
                - Embeddings are stored in PostgreSQL using PGVector.
                - When a user asks a document question, the system performs
                  vector similarity search to retrieve relevant chunks.
                - The retrieved context is sent to the local Qwen language
                  model through Ollama.
                - The embedding model used is nomic-embed-text.
                - The chatbot generates an answer using the retrieved context.
                - The application backend uses Java, Spring Boot and Spring AI.
                - The frontend uses React, TypeScript and Vite.
                - PostgreSQL with PGVector is used as the vector database.
                - The AI models run locally through Ollama.
                - The application is designed to answer questions using
                  uploaded documents and provide source information.

                TYPE 2 - DOCUMENT QUESTIONS:
                For questions about uploaded documents, use ONLY the supplied
                DOCUMENT CONTEXT.

                IMPORTANT RULES:
                1. For greetings such as "hey", "hello", or "hi", respond
                   naturally and helpfully.
                2. For project questions, use the PROJECT KNOWLEDGE above.
                3. For questions about uploaded documents, use only the
                   DOCUMENT CONTEXT.
                4. Never invent facts about uploaded documents.
                5. If a document question cannot be answered from the supplied
                   document context, say:
                   I couldn't find that information in your documents.
                6. Keep answers clear, professional and concise.
                7. If the user asks how the complete system works, explain:
                   upload -> text extraction -> chunking -> embeddings ->
                   PGVector -> similarity search -> retrieved context ->
                   Ollama/Qwen -> answer.
                """;

        return chatClient
                .prompt()
                .system(systemPrompt)
                .user("""
                        DOCUMENT CONTEXT:

                        %s

                        USER QUESTION:

                        %s
                        """.formatted(
                        context == null ? "No document context available." : context,
                        question.trim()
                ))
                .stream()
                .content();
    }

    public ChatResponse chat(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty.");
        }

        String question = message.trim();
        String lower = question.toLowerCase();

        // Greetings should NOT go through document retrieval.
        if (lower.matches("^(hi|hello|hey|hey there|hi there|good morning|good afternoon|good evening)[!. ]*$")) {
            return new ChatResponse(
                    "Hey! 👋 How can I help you with this RAG project or your documents?",
                    List.of()
            );
        }

        // Common project-review questions should use project knowledge.
        boolean projectQuestion =
                lower.contains("what is rag")
                || lower.contains("what is this project")
                || lower.contains("main purpose")
                || lower.contains("how does this project")
                || lower.contains("how does the system")
                || lower.contains("how does it work")
                || lower.contains("technology")
                || lower.contains("technologies")
                || lower.contains("tech stack")
                || lower.contains("architecture")
                || lower.contains("pgvector")
                || lower.contains("postgresql")
                || lower.contains("ollama")
                || lower.contains("embedding")
                || lower.contains("nomic")
                || lower.contains("qwen")
                || lower.contains("chunking")
                || lower.contains("chunk")
                || lower.contains("file format")
                || lower.contains("supported file")
                || lower.contains("spring boot")
                || lower.contains("spring ai")
                || lower.contains("limitation")
                || lower.contains("future improvement")
                || lower.contains("future scope")
                || lower.contains("local ai")
                || lower.contains("vector database")
                || lower.contains("vector store")
                || lower.contains("similarity search");

        if (projectQuestion) {

            String answer = chatClient
                    .prompt()
                    .system("""
                            You are the AI assistant for an AI-powered
                            Retrieval-Augmented Generation (RAG) document
                            chatbot project.

                            Project knowledge:

                            - The application is a document-grounded AI
                              chatbot.
                            - Users can upload PDF, DOCX, TXT and Markdown
                              documents.
                            - Documents are processed and split into smaller
                              chunks.
                            - Chunks are converted into vector embeddings.
                            - Embeddings are stored in PostgreSQL using
                              PGVector.
                            - User questions are matched against stored
                              vectors using similarity search.
                            - Relevant document context is sent to a local
                              Qwen language model through Ollama.
                            - The embedding model is nomic-embed-text.
                            - The backend uses Java, Spring Boot and
                              Spring AI.
                            - The frontend uses React, TypeScript and Vite.
                            - The system can return source information for
                              retrieved document content.
                            - The AI models run locally through Ollama.

                            Answer project-review questions clearly and
                            professionally.

                            If asked to explain the complete flow, explain:
                            document upload -> text extraction -> chunking ->
                            embeddings -> PGVector -> similarity search ->
                            retrieved context -> Qwen/Ollama -> final answer.

                            Keep answers concise enough for a project demo.
                            """)
                    .user(question)
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                answer = "I couldn't generate an answer.";
            }

            return new ChatResponse(
                    answer.trim(),
                    List.of()
            );
        }

        // Everything else is treated as a document question.
        List<Document> documents = retrieve(question);

        if (documents == null || documents.isEmpty()) {
            return new ChatResponse(
                    "I couldn't find that information in your documents.",
                    List.of()
            );
        }

        String context = buildContext(documents);

        String answer = chatClient
                .prompt()
                .system("""
                        You are a document-grounded AI assistant.

                        Answer the user's question using ONLY the supplied
                        document context.

                        Never invent information.
                        Do not use outside knowledge.

                        If the answer is not contained in the context,
                        respond exactly:
                        I couldn't find that information in your documents.

                        Keep the answer clear and concise.
                        """)
                .user("""
                        DOCUMENT CONTEXT:

                        %s

                        USER QUESTION:

                        %s
                        """.formatted(context, question))
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            answer = "I couldn't generate an answer.";
        }

        return new ChatResponse(
                answer.trim(),
                buildSources(documents)
        );
    }
    public List<SourceInfo> buildSources(List<Document> documents) {
        List<SourceInfo> sources = new ArrayList<>();

        for (Document document : documents) {
            Object fileName = document.getMetadata().get("fileName");
            Object page = document.getMetadata().get("page_number");
            Object documentId = document.getMetadata().get("documentId");

            sources.add(
                    new SourceInfo(
                            fileName == null ? "Unknown" : fileName.toString(),
                            page,
                            documentId == null ? null : documentId.toString()
                    )
            );
        }

        return sources;
    }
}