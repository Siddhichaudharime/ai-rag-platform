package com.rishabh.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishabh.rag.dto.ChatRequest;
import com.rishabh.rag.dto.ChatResponse;
import com.rishabh.rag.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatController(
            ChatService chatService,
            ObjectMapper objectMapper
    ) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> chat(
            @Valid @RequestBody ChatRequest request
    ) {
        try {
            ChatResponse response =
                    chatService.chat(request.message());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "INVALID_REQUEST",
                            "message", ex.getMessage()
                    ));

        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "CHAT_FAILED",
                            "message", "Unable to process your question."
                    ));
        }
    }

    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public ResponseEntity<Flux<String>> streamChat(
            @Valid @RequestBody ChatRequest request
    ) {
        try {
            String question = request.message().trim();

            List<org.springframework.ai.document.Document> documents =
                    chatService.retrieve(question);

            if (documents == null || documents.isEmpty()) {
                return ResponseEntity.ok(
                        Flux.just(
                                event(Map.of(
                                        "type", "token",
                                        "content",
                                        "I couldn't find that information in your documents."
                                )),
                                event(Map.of(
                                        "type", "sources",
                                        "sources", List.of()
                                )),
                                event(Map.of(
                                        "type", "done"
                                ))
                        )
                );
            }

            String context = chatService.buildContext(documents);

            Flux<String> tokens =
                    chatService
                            .streamAnswer(question, context)
                            .map(token ->
                                    event(Map.of(
                                            "type", "token",
                                            "content", token
                                    ))
                            );

            Flux<String> ending =
                    Flux.just(
                            event(Map.of(
                                    "type", "sources",
                                    "sources",
                                    chatService.buildSources(documents)
                            )),
                            event(Map.of(
                                    "type", "done"
                            ))
                    );

            return ResponseEntity.ok(
                    tokens.concatWith(ending)
            );

        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(
                            Flux.just(
                                    event(Map.of(
                                            "type", "error",
                                            "message",
                                            "Unable to process your question."
                                    )),
                                    event(Map.of(
                                            "type", "done"
                                    ))
                            )
                    );
        }
    }

    private String event(Object data) {
        try {
            return "data: "
                    + objectMapper.writeValueAsString(data)
                    + "\n\n";
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to create streaming response.",
                    ex
            );
        }
    }
}