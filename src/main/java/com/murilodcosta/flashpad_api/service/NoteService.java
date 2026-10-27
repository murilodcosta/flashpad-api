package com.murilodcosta.flashpad_api.service;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.PadUpdateEvent;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.entity.Note;
import com.murilodcosta.flashpad_api.exception.NoteNotFoundException;
import com.murilodcosta.flashpad_api.exception.VersionConflictException;
import com.murilodcosta.flashpad_api.repository.NoteRepository;
import com.murilodcosta.flashpad_api.util.SlugValidator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    // Thread-safe map associating each slug with its connected clients via SSE
    private final Map<String, List<SseEmitter>> emittersBySlug = new ConcurrentHashMap<>();

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public NoteResponse getNoteBySlug(String rawSlug) {
        String slug = SlugValidator.validate(rawSlug);
        return noteRepository.findBySlug(slug)
                .map(NoteResponse::fromEntity)
                .orElseThrow(() -> new NoteNotFoundException("Note not found with slug: " + slug));
    }

    @Transactional
    public NoteResponse upsertNote(String rawSlug, UpdateNoteRequest request) {
        String slug = SlugValidator.validate(rawSlug);
        String content = request != null ? request.content() : null;
        Long expectedVersion = request != null ? request.version() : null;
        String clientId = request != null ? request.clientId() : null;

        Note note = noteRepository.findBySlug(slug)
                .map(existingNote -> {
                    if (!Objects.equals(expectedVersion, existingNote.getVersion())) {
                        throw new VersionConflictException(existingNote.getVersion(), existingNote.getContent());
                    }
                    existingNote.setContent(content);
                    return existingNote;
                })
                .orElseGet(() -> new Note(slug, content));

        Note savedNote = noteRepository.saveAndFlush(note);
        NoteResponse response = NoteResponse.fromEntity(savedNote);

        // Notify all clients subscribed to this pad slug with clientId for echo filtering
        broadcastPadUpdate(slug, new PadUpdateEvent(savedNote.getContent(), savedNote.getVersion(), clientId));

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
     * Periodically send a heartbeat ping comment to keep SSE connections open behind reverse proxies.
     */
    @Scheduled(fixedDelay = 25_000)
    public void sendHeartbeat() {
        for (Map.Entry<String, List<SseEmitter>> entry : emittersBySlug.entrySet()) {
            String slug = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    emitter.complete();
                    removeEmitter(slug, emitter);
                }
            }
        }
    }

    /**
     * Broadcast pad update event named 'pad-updated' to all clients connected to the corresponding slug.
     */
    private void broadcastPadUpdate(String slug, PadUpdateEvent event) {
        List<SseEmitter> emitters = emittersBySlug.get(slug);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("pad-updated")
                        .data(event));
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
