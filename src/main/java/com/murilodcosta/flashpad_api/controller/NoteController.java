package com.murilodcosta.flashpad_api.controller;

import com.murilodcosta.flashpad_api.repository.NoteRepository;
import com.murilodcosta.flashpad_api.service.NoteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }


}
