package com.murilodcosta.flashpad_api.controller;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/{*path}")
    public NoteResponse getNoteByPath(@PathVariable String path) {
        return noteService.getNoteByPath(path);
    }

    @PutMapping("/{*path}")
    public NoteResponse upsertNote(
            @PathVariable String path,
            @Valid @RequestBody(required = false) UpdateNoteRequest request
    ) {
        return noteService.upsertNote(path, request);
    }

    /**
     * Subscribe to Server-Sent Events (SSE) stream to receive real-time updates for the note.
     */
    @GetMapping(value = "/{path}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNoteUpdates(@PathVariable String path) {
        return noteService.subscribeToNoteStream(path);
    }
}
