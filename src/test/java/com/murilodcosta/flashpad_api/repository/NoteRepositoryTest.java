package com.murilodcosta.flashpad_api.repository;

import com.murilodcosta.flashpad_api.entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoteRepositoryTest {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
    }

    @Test
    @DisplayName("Should find a note by path when it exists")
    void shouldFindNoteByPathWhenExists() {
        Note note = new Note("/my-note", "Sample content");
        entityManager.persistAndFlush(note);

        Optional<Note> found = noteRepository.findByPath("/my-note");

        assertThat(found).isPresent();
        assertThat(found.get().getPath()).isEqualTo("/my-note");
        assertThat(found.get().getContent()).isEqualTo("Sample content");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty Optional when path does not exist")
    void shouldReturnEmptyWhenPathNotFound() {
        Optional<Note> found = noteRepository.findByPath("/non-existent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should save a new note with createdAt populated")
    void shouldSaveNewNote() {
        Note note = new Note("/shopping-list", "Eggs, Milk, Bread");
        Note savedNote = noteRepository.saveAndFlush(note);

        assertThat(savedNote.getId()).isNotNull();
        assertThat(savedNote.getPath()).isEqualTo("/shopping-list");
        assertThat(savedNote.getContent()).isEqualTo("Eggs, Milk, Bread");
        assertThat(savedNote.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update an existing note and populate updatedAt")
    void shouldUpdateNoteContent() {
        Note note = new Note("/project-notes", "Initial version");
        Note savedNote = noteRepository.saveAndFlush(note);

        savedNote.setContent("Updated version");
        Note updatedNote = noteRepository.saveAndFlush(savedNote);

        assertThat(updatedNote.getContent()).isEqualTo("Updated version");
        assertThat(updatedNote.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce uniqueness on path column")
    void shouldThrowExceptionWhenDuplicatePath() {
        Note first = new Note("/duplicate-test", "First content");
        entityManager.persistAndFlush(first);

        Note second = new Note("/duplicate-test", "Second content");

        assertThatThrownBy(() -> {
            noteRepository.saveAndFlush(second);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}