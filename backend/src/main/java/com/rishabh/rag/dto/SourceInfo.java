package com.rishabh.rag.dto;

public record SourceInfo(
        String fileName,
        Object page,
        String documentId
) {
}
