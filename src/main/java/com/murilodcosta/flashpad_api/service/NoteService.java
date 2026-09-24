package com.murilodcosta.flashpad_api.service;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.entity.Note;
import com.murilodcosta.flashpad_api.exception.NoteNotFoundException;
import com.murilodcosta.flashpad_api.repository.NoteRepository;
import com.murilodcosta.flashpad_api.util.PathNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

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
        return NoteResponse.fromEntity(savedNote);
    }
}
