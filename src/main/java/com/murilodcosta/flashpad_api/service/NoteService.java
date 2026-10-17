package com.murilodcosta.flashpad_api.service;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.entity.Note;
import com.murilodcosta.flashpad_api.exception.NoteNotFoundException;
import com.murilodcosta.flashpad_api.repository.NoteRepository;
import com.murilodcosta.flashpad_api.util.SlugValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    // thread-safe map associating each slug with its connected clients via SSE
    private final Map<String, List<SseEmitter>> emittersBySlug = new ConcurrentHashMap<>();

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public NoteResponse getNoteBySlug(String rawSlug) {
        String slug = SlugValidator.validate(rawSlug);
        return noteRepository.findByPath(slug)
                .map(NoteResponse::fromEntity)
                .orElseThrow(() -> new NoteNotFoundException("Note not found with slug: " + slug));
    }

    @Transactional(readOnly = true)
    public NoteResponse getNoteByPath(String rawPath) {
        return getNoteBySlug(rawPath);
    }

    @Transactional
    public NoteResponse upsertNote(String rawSlug, UpdateNoteRequest request) {
        String slug = SlugValidator.validate(rawSlug);
        String content = request != null ? request.content() : null;

        Note note = noteRepository.findByPath(slug)
                .map(existingNote -> {
                    existingNote.setContent(content);
                    return existingNote;
                })
                .orElseGet(() -> new Note(slug, content));

        Note savedNote = noteRepository.save(note);
        NoteResponse response = NoteResponse.fromEntity(savedNote);

        // Notify all clients subscribed to this specific note slug about the update
        broadcastNoteUpdate(slug, response);

        return response;
    }

    /**
     * Register a new SSE connection to receive updates for the note with the specified slug.
     */
    public SseEmitter subscribeToNoteStream(String rawSlug) {
        String slug = SlugValidator.validate(rawSlug);
        SseEmitter emitter = new SseEmitter(180_000L); // Default 3-minute timeout

        emittersBySlug.computeIfAbsent(slug, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Remove the emitter from the list when the connection is completed, timed out, or encounters an error
        Runnable cleanup = () -> removeEmitter(slug, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(throwable -> cleanup.run());

        return emitter;
    }

    /**
     * Send the updated note data to all clients connected to the corresponding slug.
     */
    private void broadcastNoteUpdate(String slug, NoteResponse response) {
        List<SseEmitter> emitters = emittersBySlug.get(slug);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(response);
            } catch (IOException | IllegalStateException e) {
                emitter.complete();
                removeEmitter(slug, emitter);
            }
        }
    }

    /**
     * Remove an emitter from the list for a specific slug to prevent memory leaks.
     */
    private void removeEmitter(String slug, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersBySlug.get(slug);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersBySlug.remove(slug);
            }
        }
    }
}
