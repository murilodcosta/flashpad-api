package com.murilodcosta.flashpad_api.dto;

import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
        @Size(max = 100000, message = "Content cannot exceed 100,000 characters")
        String content,
        Long version,
        String clientId
) {
    public UpdateNoteRequest(String content) {
        this(content, null, null);
    }
}
