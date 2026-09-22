package com.murilodcosta.flashpad_api.repository;

import com.murilodcosta.flashpad_api.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByPath(String path);
}
