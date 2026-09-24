package com.murilodcosta.flashpad_api.dto;

import com.murilodcosta.flashpad_api.entity.Note;

import java.time.LocalDateTime;

public record NoteResponse(
        String path,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoteResponse fromEntity(Note note) {
        return new NoteResponse(
                note.getPath(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
