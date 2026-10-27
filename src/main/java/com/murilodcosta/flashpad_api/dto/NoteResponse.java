package com.murilodcosta.flashpad_api.dto;

import com.murilodcosta.flashpad_api.entity.Note;

import java.time.LocalDateTime;

public record NoteResponse(
        String slug,
        String content,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoteResponse fromEntity(Note note) {
        return new NoteResponse(
                note.getSlug(),
                note.getContent(),
                note.getVersion(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
