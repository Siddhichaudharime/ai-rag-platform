package com.rishabh.rag.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rishabh.rag.model.Document;
import com.rishabh.rag.repository.DocumentRepository;
import com.rishabh.rag.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    public DocumentController(
            DocumentService documentService,
            DocumentRepository documentRepository
    ) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file
    ) {

        try {

            Document document =
                    documentService.uploadAndIndex(file);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(document);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", "INVALID_DOCUMENT",
                            "message", ex.getMessage()
                    ));

        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "DOCUMENT_PROCESSING_FAILED",
                            "message", ex.getMessage()
                    ));
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> getDocuments() {

        return ResponseEntity.ok(
                documentRepository.findAll()
        );
    }
}