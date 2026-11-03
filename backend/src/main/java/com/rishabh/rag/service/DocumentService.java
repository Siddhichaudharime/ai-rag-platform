package com.rishabh.rag.service;

import com.rishabh.rag.model.DocumentStatus;
import com.rishabh.rag.repository.DocumentRepository;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

    private final TokenTextSplitter textSplitter = TokenTextSplitter.builder()
            .withChunkSize(800)
            .withMinChunkSizeChars(350)
            .withMinChunkLengthToEmbed(10)
            .withMaxNumChunks(10_000)
            .withKeepSeparator(true)
            .build();

    public DocumentService(
            DocumentRepository documentRepository,
            VectorStore vectorStore
    ) {
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    public com.rishabh.rag.model.Document uploadAndIndex(
            MultipartFile file
    ) {

        validateFile(file);

        String fileName = sanitizeFileName(
                file.getOriginalFilename()
        );

        String contentType = file.getContentType() != null
                ? file.getContentType()
                : "application/octet-stream";

        com.rishabh.rag.model.Document savedDocument =
                new com.rishabh.rag.model.Document(
                        fileName,
                        contentType,
                        file.getSize(),
                        DocumentStatus.UPLOADED
                );

        savedDocument = documentRepository.save(savedDocument);

        UUID documentId = savedDocument.getId();

        Path temporaryFile = null;

        try {

            savedDocument.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(savedDocument);

            String extension = getExtension(fileName);

            temporaryFile = Files.createTempFile(
                    "rag-document-",
                    extension.isBlank()
                            ? ".tmp"
                            : "." + extension
            );

            file.transferTo(temporaryFile);

            FileSystemResource resource =
                    new FileSystemResource(temporaryFile);

            List<org.springframework.ai.document.Document>
                    sourceDocuments =
                    readDocument(resource, extension);

            if (sourceDocuments.isEmpty()) {
                throw new IllegalArgumentException(
                        "The uploaded document does not contain readable text."
                );
            }

            List<org.springframework.ai.document.Document> chunks =
                    textSplitter.split(sourceDocuments);

            if (chunks.isEmpty()) {
                throw new IllegalArgumentException(
                        "The uploaded document produced no usable text chunks."
                );
            }

            List<org.springframework.ai.document.Document>
                    enrichedChunks =
                    new ArrayList<>(chunks.size());

            for (
                    org.springframework.ai.document.Document chunk
                    : chunks
            ) {

                org.springframework.ai.document.Document enrichedChunk =
                        chunk.mutate()
                                .metadata(
                                        "documentId",
                                        documentId.toString()
                                )
                                .metadata(
                                        "fileName",
                                        fileName
                                )
                                .metadata(
                                        "contentType",
                                        contentType
                                )
                                .build();

                enrichedChunks.add(enrichedChunk);
            }

            vectorStore.add(enrichedChunks);

            savedDocument.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(savedDocument);

            return savedDocument;

        }
        catch (Exception ex) {

            savedDocument.setStatus(DocumentStatus.FAILED);
            documentRepository.save(savedDocument);

            throw new IllegalStateException(
                    "Unable to process document: "
                            + ex.getMessage(),
                    ex
            );

        }
        finally {

            if (temporaryFile != null) {

                try {
                    Files.deleteIfExists(temporaryFile);
                }
                catch (IOException ignored) {
                    // Ignore temporary-file cleanup failure.
                }
            }
        }
    }

    private List<org.springframework.ai.document.Document>
    readDocument(
            FileSystemResource resource,
            String extension
    ) {

        DocumentReader reader;

        switch (extension.toLowerCase(Locale.ROOT)) {

            case "pdf":
                reader = new PagePdfDocumentReader(resource);
                break;

            case "docx":
                reader = new TikaDocumentReader(resource);
                break;

            case "txt":
                reader = new TextReader(resource);
                break;

            case "md":
                reader = new MarkdownDocumentReader(
                        resource,
                        MarkdownDocumentReaderConfig.defaultConfig()
                );
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported file type: ." + extension
                );
        }

        return reader.read();
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please select a document to upload."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB."
            );
        }

        String fileName =
                sanitizeFileName(file.getOriginalFilename());

        String extension = getExtension(fileName);

        if (!List.of("pdf", "docx", "txt", "md")
                .contains(extension.toLowerCase(Locale.ROOT))) {

            throw new IllegalArgumentException(
                    "Supported file types are PDF, DOCX, TXT and MD."
            );
        }
    }

    private String sanitizeFileName(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return "unnamed-document";
        }

        return Path.of(fileName)
                .getFileName()
                .toString();
    }

    private String getExtension(String fileName) {

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1);
    }
}