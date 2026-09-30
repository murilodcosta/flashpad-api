package com.murilodcosta.flashpad_api.service;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.entity.Note;
import com.murilodcosta.flashpad_api.exception.NoteNotFoundException;
import com.murilodcosta.flashpad_api.repository.NoteRepository;
import com.murilodcosta.flashpad_api.util.PathNormalizer;
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

    // thread-safe map associating each normalized path with its connected clients via SSE
    private final Map<String, List<SseEmitter>> emittersByPath = new ConcurrentHashMap<>();

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public NoteResponse getNoteByPath(String rawPath) {
        String normalizedPath = PathNormalizer.normalize(rawPath);
        return noteRepository.findByPath(normalizedPath)
                .map(NoteResponse::fromEntity)
                .orElseThrow(() -> new NoteNotFoundException("Note not found at path: " + normalizedPath));
    }

    @Transactional
    public NoteResponse upsertNote(String rawPath, UpdateNoteRequest request) {
        String normalizedPath = PathNormalizer.normalize(rawPath);
        String content = request != null ? request.content() : null;

        Note note = noteRepository.findByPath(normalizedPath)
                .map(existingNote -> {
                    existingNote.setContent(content);
                    return existingNote;
                })
                .orElseGet(() -> new Note(normalizedPath, content));

        Note savedNote = noteRepository.save(note);
        NoteResponse response = NoteResponse.fromEntity(savedNote);

        // Notify all clients subscribed to this specific note path about the update
        broadcastNoteUpdate(normalizedPath, response);

        return response;
    }

    /**
     * Register a new SSE connection to receive updates for the note at the specified path.
     */
    public SseEmitter subscribeToNoteStream(String rawPath) {
        String normalizedPath = PathNormalizer.normalize(rawPath);
        SseEmitter emitter = new SseEmitter(180_000L); // Default 3-minute timeout

        emittersByPath.computeIfAbsent(normalizedPath, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Remove the emitter from the list when the connection is completed, timed out, or encounters an error
        Runnable cleanup = () -> removeEmitter(normalizedPath, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(throwable -> cleanup.run());

        return emitter;
    }

    /**
     * Send the updated note data to all clients connected to the corresponding path.
     */
    private void broadcastNoteUpdate(String normalizedPath, NoteResponse response) {
        List<SseEmitter> emitters = emittersByPath.get(normalizedPath);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(response);
            } catch (IOException | IllegalStateException e) {
                emitter.complete();
                removeEmitter(normalizedPath, emitter);
            }
        }
    }

    /**
     * Remove an emitter from the list for a specific path to prevent memory leaks.
     */
    private void removeEmitter(String normalizedPath, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByPath.get(normalizedPath);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByPath.remove(normalizedPath);
            }
        }
    }
}
