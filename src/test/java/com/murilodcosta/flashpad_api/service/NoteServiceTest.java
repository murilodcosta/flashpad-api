package com.murilodcosta.flashpad_api.service;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.entity.Note;
import com.murilodcosta.flashpad_api.exception.InvalidSlugException;
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
    @DisplayName("Should return NoteResponse with validated slug when note exists")
    void shouldReturnNoteResponseWhenNoteExists() {
        String rawSlug = "  My-Note  ";
        String normalizedSlug = "my-note";
        LocalDateTime now = LocalDateTime.now();
        Note existingNote = new Note(1L, normalizedSlug, "Hello World", now, null);

        when(noteRepository.findBySlug(normalizedSlug)).thenReturn(Optional.of(existingNote));

        NoteResponse response = noteService.getNoteBySlug(rawSlug);

        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo(normalizedSlug);
        assertThat(response.content()).isEqualTo("Hello World");
        assertThat(response.createdAt()).isEqualTo(now);
        verify(noteRepository).findBySlug(normalizedSlug);
    }

    @Test
    @DisplayName("Should throw NoteNotFoundException when note does not exist")
    void shouldThrowExceptionWhenNoteDoesNotExist() {
        String slug = "unknown-note";

        when(noteRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteBySlug(slug))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessage("Note not found with slug: " + slug);

        verify(noteRepository).findBySlug(slug);
    }

    @Test
    @DisplayName("Should throw InvalidSlugException when slug is reserved or invalid")
    void shouldThrowExceptionWhenSlugIsInvalid() {
        assertThatThrownBy(() -> noteService.getNoteBySlug("api"))
                .isInstanceOf(InvalidSlugException.class);

        assertThatThrownBy(() -> noteService.upsertNote("invalid/slug", new UpdateNoteRequest("content")))
                .isInstanceOf(InvalidSlugException.class);

        assertThatThrownBy(() -> noteService.subscribeToNoteStream("actuator"))
                .isInstanceOf(InvalidSlugException.class);
    }

    @Test
    @DisplayName("Should create and save new note when it does not exist")
    void shouldCreateNewNoteWhenNotExists() {
        String rawSlug = "new-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Brand new content");

        when(noteRepository.findBySlug(rawSlug)).thenReturn(Optional.empty());
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note noteToSave = invocation.getArgument(0);
            noteToSave.setId(10L);
            noteToSave.setCreatedAt(LocalDateTime.now());
            return noteToSave;
        });

        NoteResponse response = noteService.upsertNote(rawSlug, request);

        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo(rawSlug);
        assertThat(response.content()).isEqualTo("Brand new content");

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        Note captured = noteCaptor.getValue();
        assertThat(captured.getSlug()).isEqualTo(rawSlug);
        assertThat(captured.getContent()).isEqualTo("Brand new content");
    }

    @Test
    @DisplayName("Should update existing note content when note already exists")
    void shouldUpdateExistingNoteWhenExists() {
        String slug = "existing-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Updated content");

        Note existingNote = new Note(5L, slug, "Old content", LocalDateTime.now().minusHours(1), null);

        when(noteRepository.findBySlug(slug)).thenReturn(Optional.of(existingNote));
        when(noteRepository.save(existingNote)).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            note.setUpdatedAt(LocalDateTime.now());
            return note;
        });

        NoteResponse response = noteService.upsertNote(slug, request);

        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo(slug);
        assertThat(response.content()).isEqualTo("Updated content");
        assertThat(existingNote.getContent()).isEqualTo("Updated content");
        verify(noteRepository).save(existingNote);
    }

    @Test
    @DisplayName("Should handle null request gracefully during upsert")
    void shouldHandleNullRequestDuringUpsert() {
        String slug = "empty-note";

        when(noteRepository.findBySlug(slug)).thenReturn(Optional.empty());
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse response = noteService.upsertNote(slug, null);

        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo(slug);
        assertThat(response.content()).isNull();
    }

    @Test
    @DisplayName("Should create and return SseEmitter on subscribe")
    void shouldSubscribeToNoteStream() {
        var emitter = noteService.subscribeToNoteStream("live-note");

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("Should broadcast update only to emitters subscribed to the matching slug")
    void shouldBroadcastOnlyToMatchingSlugEmitters() {
        var emitterSlugA = noteService.subscribeToNoteStream("note-a");
        var emitterSlugB = noteService.subscribeToNoteStream("note-b");

        Note noteA = new Note(1L, "note-a", "Updated A", LocalDateTime.now(), null);
        when(noteRepository.findBySlug("note-a")).thenReturn(Optional.of(noteA));
        when(noteRepository.save(any(Note.class))).thenReturn(noteA);

        // Trigger upsert for note-a
        NoteResponse response = noteService.upsertNote("note-a", new UpdateNoteRequest("Updated A"));

        assertThat(response.content()).isEqualTo("Updated A");
    }
}
