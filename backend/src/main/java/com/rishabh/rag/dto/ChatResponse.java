package com.rishabh.rag.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<SourceInfo> sources
) {
}
