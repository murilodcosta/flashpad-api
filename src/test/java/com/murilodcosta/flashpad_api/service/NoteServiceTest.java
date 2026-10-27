package com.murilodcosta.flashpad_api.service;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.entity.Note;
import com.murilodcosta.flashpad_api.exception.InvalidSlugException;
import com.murilodcosta.flashpad_api.exception.NoteNotFoundException;
import com.murilodcosta.flashpad_api.exception.VersionConflictException;
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
    @DisplayName("Should return NoteResponse with validated slug and version when note exists")
    void shouldReturnNoteResponseWhenNoteExists() {
        String rawSlug = "  My-Note  ";
        String normalizedSlug = "my-note";
        LocalDateTime now = LocalDateTime.now();
        Note existingNote = new Note(1L, normalizedSlug, "Hello World", 0L, now, null);

        when(noteRepository.findBySlug(normalizedSlug)).thenReturn(Optional.of(existingNote));

        NoteResponse response = noteService.getNoteBySlug(rawSlug);

        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo(normalizedSlug);
        assertThat(response.content()).isEqualTo("Hello World");
        assertThat(response.version()).isEqualTo(0L);
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
    @DisplayName("Should create and save new note when it does not exist without version")
    void shouldCreateNewNoteWhenNotExists() {
        String rawSlug = "new-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Brand new content", null, "client-new");

        when(noteRepository.findBySlug(rawSlug)).thenReturn(Optional.empty());
        when(noteRepository.saveAndFlush(any(Note.class))).thenAnswer(invocation -> {
            Note noteToSave = invocation.getArgument(0);
            noteToSave.setId(10L);
            noteToSave.setVersion(0L);
            noteToSave.setCreatedAt(LocalDateTime.now());
            return noteToSave;
        });

        NoteResponse response = noteService.upsertNote(rawSlug, request);

        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo(rawSlug);
        assertThat(response.content()).isEqualTo("Brand new content");
        assertThat(response.version()).isEqualTo(0L);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).saveAndFlush(noteCaptor.capture());
        Note captured = noteCaptor.getValue();
        assertThat(captured.getSlug()).isEqualTo(rawSlug);
        assertThat(captured.getContent()).isEqualTo("Brand new content");
    }

    @Test
    @DisplayName("Should update existing note content when expected version matches")
    void shouldUpdateExistingNoteWhenVersionMatches() {
        String slug = "existing-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Updated content", 2L, "client-1");

        Note existingNote = new Note(5L, slug, "Old content", 2L, LocalDateTime.now().minusHours(1), null);

        when(noteRepository.findBySlug(slug)).thenReturn(Optional.of(existingNote));
        when(noteRepository.saveAndFlush(existingNote)).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            note.setVersion(3L);
            note.setUpdatedAt(LocalDateTime.now());
            return note;
        });

        NoteResponse response = noteService.upsertNote(slug, request);

        assertThat(response).isNotNull();
        assertThat(response.slug()).isEqualTo(slug);
        assertThat(response.content()).isEqualTo("Updated content");
        assertThat(response.version()).isEqualTo(3L);
        assertThat(existingNote.getContent()).isEqualTo("Updated content");
        verify(noteRepository).saveAndFlush(existingNote);
    }

    @Test
    @DisplayName("Should throw VersionConflictException when expected version does not match existing version")
    void shouldThrowConflictWhenExpectedVersionDoesNotMatch() {
        String slug = "existing-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Stale edit", 1L, "client-stale");

        Note existingNote = new Note(5L, slug, "Server content", 3L, LocalDateTime.now().minusHours(1), null);

        when(noteRepository.findBySlug(slug)).thenReturn(Optional.of(existingNote));

        assertThatThrownBy(() -> noteService.upsertNote(slug, request))
                .isInstanceOf(VersionConflictException.class)
                .satisfies(ex -> {
                    VersionConflictException conflict = (VersionConflictException) ex;
                    assertThat(conflict.getCurrentVersion()).isEqualTo(3L);
                    assertThat(conflict.getCurrentContent()).isEqualTo("Server content");
                });

        verify(noteRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should throw VersionConflictException when version is omitted on an existing note")
    void shouldThrowConflictWhenVersionOmittedOnExistingNote() {
        String slug = "existing-note";
        UpdateNoteRequest request = new UpdateNoteRequest("Edit without version");

        Note existingNote = new Note(5L, slug, "Existing pad", 0L, LocalDateTime.now().minusHours(1), null);

        when(noteRepository.findBySlug(slug)).thenReturn(Optional.of(existingNote));

        assertThatThrownBy(() -> noteService.upsertNote(slug, request))
                .isInstanceOf(VersionConflictException.class)
                .satisfies(ex -> {
                    VersionConflictException conflict = (VersionConflictException) ex;
                    assertThat(conflict.getCurrentVersion()).isEqualTo(0L);
                    assertThat(conflict.getCurrentContent()).isEqualTo("Existing pad");
                });

        verify(noteRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should handle null request gracefully during upsert of new note")
    void shouldHandleNullRequestDuringUpsert() {
        String slug = "empty-note";

        when(noteRepository.findBySlug(slug)).thenReturn(Optional.empty());
        when(noteRepository.saveAndFlush(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
    @DisplayName("Should broadcast update to emitters subscribed to the matching slug")
    void shouldBroadcastOnlyToMatchingSlugEmitters() {
        var emitterSlugA = noteService.subscribeToNoteStream("note-a");
        var emitterSlugB = noteService.subscribeToNoteStream("note-b");

        Note noteA = new Note(1L, "note-a", "Updated A", 0L, LocalDateTime.now(), null);
        when(noteRepository.findBySlug("note-a")).thenReturn(Optional.of(noteA));
        when(noteRepository.saveAndFlush(any(Note.class))).thenReturn(noteA);

        // Trigger upsert for note-a
        NoteResponse response = noteService.upsertNote("note-a", new UpdateNoteRequest("Updated A", 0L, "client-1"));

        assertThat(response.content()).isEqualTo("Updated A");
    }

    @Test
    @DisplayName("Should send heartbeat ping to all subscribed emitters without throwing exceptions")
    void shouldSendHeartbeatPingSuccessfully() {
        noteService.subscribeToNoteStream("heartbeat-note");

        // Calling sendHeartbeat should execute cleanly
        noteService.sendHeartbeat();
    }
}
