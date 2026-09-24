package com.murilodcosta.flashpad_api.service;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.entity.Note;
import com.murilodcosta.flashpad_api.exception.NoteNotFoundException;
import com.murilodcosta.flashpad_api.repository.NoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private NoteService noteService;

    @Test
    @DisplayName("Should return NoteResponse with normalized path when note exists")
    void shouldReturnNoteResponseWhenNoteExists() {
        String rawPath = "my-note";
        String normalizedPath = "/my-note";
        LocalDateTime now = LocalDateTime.now();
        Note existingNote = new Note(1L, normalizedPath, "Hello World", now, null);

        when(noteRepository.findByPath(normalizedPath)).thenReturn(Optional.of(existingNote));

        NoteResponse response = noteService.getNoteByPath(rawPath);

        assertThat(response).isNotNull();
        assertThat(response.path()).isEqualTo(normalizedPath);
        assertThat(response.content()).isEqualTo("Hello World");
        assertThat(response.createdAt()).isEqualTo(now);
        verify(noteRepository).findByPath(normalizedPath);
    }

    @Test
    @DisplayName("Should throw NoteNotFoundException when note does not exist")
    void shouldThrowExceptionWhenNoteDoesNotExist() {
        String rawPath = "unknown-note";
        String normalizedPath = "/unknown-note";

        when(noteRepository.findByPath(normalizedPath)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteByPath(rawPath))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessage("Note not found at path: " + normalizedPath);

        verify(noteRepository).findByPath(normalizedPath);
    }

    @Test
    @DisplayName("Should create and save new note when it does not exist")
    void shouldCreateNewNoteWhenNotExists() {
        String rawPath = "new-note";
        String normalizedPath = "/new-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Brand new content");

        when(noteRepository.findByPath(normalizedPath)).thenReturn(Optional.empty());
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note noteToSave = invocation.getArgument(0);
            noteToSave.setId(10L);
            noteToSave.setCreatedAt(LocalDateTime.now());
            return noteToSave;
        });

        NoteResponse response = noteService.upsertNote(rawPath, request);

        assertThat(response).isNotNull();
        assertThat(response.path()).isEqualTo(normalizedPath);
        assertThat(response.content()).isEqualTo("Brand new content");

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        Note captured = noteCaptor.getValue();
        assertThat(captured.getPath()).isEqualTo(normalizedPath);
        assertThat(captured.getContent()).isEqualTo("Brand new content");
    }

    @Test
    @DisplayName("Should update existing note content when note already exists")
    void shouldUpdateExistingNoteWhenExists() {
        String rawPath = "existing-note";
        String normalizedPath = "/existing-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Updated content");

        Note existingNote = new Note(5L, normalizedPath, "Old content", LocalDateTime.now().minusHours(1), null);

        when(noteRepository.findByPath(normalizedPath)).thenReturn(Optional.of(existingNote));
        when(noteRepository.save(existingNote)).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            note.setUpdatedAt(LocalDateTime.now());
            return note;
        });

        NoteResponse response = noteService.upsertNote(rawPath, request);

        assertThat(response).isNotNull();
        assertThat(response.path()).isEqualTo(normalizedPath);
        assertThat(response.content()).isEqualTo("Updated content");
        assertThat(existingNote.getContent()).isEqualTo("Updated content");
        verify(noteRepository).save(existingNote);
    }

    @Test
    @DisplayName("Should handle null request gracefully during upsert")
    void shouldHandleNullRequestDuringUpsert() {
        String rawPath = "empty-note";
        String normalizedPath = "/empty-note";

        when(noteRepository.findByPath(normalizedPath)).thenReturn(Optional.empty());
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse response = noteService.upsertNote(rawPath, null);

        assertThat(response).isNotNull();
        assertThat(response.path()).isEqualTo(normalizedPath);
        assertThat(response.content()).isNull();
    }
}
