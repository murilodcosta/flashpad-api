package com.murilodcosta.flashpad_api.controller;

import com.murilodcosta.flashpad_api.dto.NoteResponse;
import com.murilodcosta.flashpad_api.dto.UpdateNoteRequest;
import com.murilodcosta.flashpad_api.exception.GlobalExceptionHandler;
import com.murilodcosta.flashpad_api.exception.NoteNotFoundException;
import com.murilodcosta.flashpad_api.service.NoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {NoteController.class, GlobalExceptionHandler.class})
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService noteService;

    @Test
    @DisplayName("GET /api/v1/notes/{path} should return 200 and NoteResponse for simple path")
    void shouldReturnNoteForSimplePath() throws Exception {
        NoteResponse response = new NoteResponse(
                "/test-note",
                "Hello World",
                LocalDateTime.of(2026, 9, 28, 12, 0),
                null
        );

        when(noteService.getNoteByPath("/test-note")).thenReturn(response);

        mockMvc.perform(get("/api/v1/notes/test-note"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("/test-note"))
                .andExpect(jsonPath("$.content").value("Hello World"));

        verify(noteService).getNoteByPath("/test-note");
    }

    @Test
    @DisplayName("GET /api/v1/notes/{*path} should return 200 for nested multi-level path")
    void shouldReturnNoteForNestedPath() throws Exception {
        NoteResponse response = new NoteResponse(
                "/docs/chapter-1/page-42",
                "Nested documentation content",
                LocalDateTime.of(2026, 9, 28, 12, 0),
                null
        );

        when(noteService.getNoteByPath("/docs/chapter-1/page-42")).thenReturn(response);

        mockMvc.perform(get("/api/v1/notes/docs/chapter-1/page-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("/docs/chapter-1/page-42"))
                .andExpect(jsonPath("$.content").value("Nested documentation content"));

        verify(noteService).getNoteByPath("/docs/chapter-1/page-42");
    }

    @Test
    @DisplayName("GET /api/v1/notes/{path} should return 404 ProblemDetail when note does not exist")
    void shouldReturn404WhenNoteNotFound() throws Exception {
        when(noteService.getNoteByPath("/unknown"))
                .thenThrow(new NoteNotFoundException("Note not found at path: /unknown"));

        mockMvc.perform(get("/api/v1/notes/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Note Not Found"))
                .andExpect(jsonPath("$.detail").value("Note not found at path: /unknown"))
                .andExpect(jsonPath("$.status").value(404));

        verify(noteService).getNoteByPath("/unknown");
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{path} should parse JSON body into UpdateNoteRequest correctly")
    void shouldUpsertNoteWithJsonBody() throws Exception {
        NoteResponse response = new NoteResponse(
                "/my-note",
                "Saved content",
                LocalDateTime.of(2026, 9, 28, 12, 0),
                LocalDateTime.of(2026, 9, 28, 12, 5)
        );

        when(noteService.upsertNote(eq("/my-note"), any(UpdateNoteRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/notes/my-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Saved content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("/my-note"))
                .andExpect(jsonPath("$.content").value("Saved content"));

        ArgumentCaptor<UpdateNoteRequest> captor = ArgumentCaptor.forClass(UpdateNoteRequest.class);
        verify(noteService).upsertNote(eq("/my-note"), captor.capture());
        assertThat(captor.getValue().content()).isEqualTo("Saved content");
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{*path} should support nested multi-level path on upsert")
    void shouldUpsertNoteWithNestedPath() throws Exception {
        NoteResponse response = new NoteResponse(
                "/folder/subfolder/my-note",
                "Deep note content",
                LocalDateTime.of(2026, 9, 28, 12, 0),
                null
        );

        when(noteService.upsertNote(eq("/folder/subfolder/my-note"), any(UpdateNoteRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/notes/folder/subfolder/my-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Deep note content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("/folder/subfolder/my-note"))
                .andExpect(jsonPath("$.content").value("Deep note content"));

        verify(noteService).upsertNote(eq("/folder/subfolder/my-note"), any(UpdateNoteRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{path} without request body should pass null request to service")
    void shouldUpsertNoteWithoutBody() throws Exception {
        NoteResponse response = new NoteResponse(
                "/empty-note",
                null,
                LocalDateTime.of(2026, 9, 28, 12, 0),
                null
        );

        when(noteService.upsertNote(eq("/empty-note"), eq(null)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/notes/empty-note"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("/empty-note"))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(noteService).upsertNote("/empty-note", null);
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{path} with content exceeding max length should return 400 Bad Request")
    void shouldReturn400WhenContentExceedsMaxLength() throws Exception {
        String oversizedContent = "a".repeat(100_001);

        mockMvc.perform(put("/api/v1/notes/oversized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + oversizedContent + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.content").value("Content cannot exceed 100,000 characters"));
    }

    @Test
    @DisplayName("GET /api/v1/notes/{path}/stream should subscribe to SSE and return 200")
    void shouldSubscribeToNoteStream() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(noteService.subscribeToNoteStream("my-note")).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/notes/my-note/stream"))
                .andExpect(status().isOk());

        verify(noteService).subscribeToNoteStream("my-note");
    }
}
