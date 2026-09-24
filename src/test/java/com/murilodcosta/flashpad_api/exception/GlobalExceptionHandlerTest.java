package com.murilodcosta.flashpad_api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 ProblemDetail for NoteNotFoundException")
    void shouldHandleNoteNotFoundException() {
        NoteNotFoundException exception = new NoteNotFoundException("Note not found at path: /test");

        ProblemDetail problemDetail = exceptionHandler.handleNoteNotFoundException(exception);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Note Not Found");
        assertThat(problemDetail.getDetail()).isEqualTo("Note not found at path: /test");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail with field errors for MethodArgumentNotValidException")
    @SuppressWarnings("unchecked")
    void shouldHandleValidationException() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError("updateNoteRequest", "content", "Content cannot exceed 100,000 characters");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problemDetail = exceptionHandler.handleValidationException(exception);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Bad Request");

        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertThat(errors).containsEntry("content", "Content cannot exceed 100,000 characters");
    }

    @Test
    @DisplayName("Should return 500 ProblemDetail for generic Exception")
    void shouldHandleGenericException() {
        Exception exception = new RuntimeException("Database timeout");

        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
}
