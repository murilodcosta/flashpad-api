package com.murilodcosta.flashpad_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoteNotFoundException.class)
    public ProblemDetail handleNoteNotFoundException(NoteNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Note Not Found");
        problemDetail.setType(URI.create("https://flashpad.dev/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InvalidSlugException.class)
    public ProblemDetail handleInvalidSlugException(InvalidSlugException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Slug");
        problemDetail.setType(URI.create("https://flashpad.dev/errors/invalid-slug"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(VersionConflictException.class)
    public ProblemDetail handleVersionConflictException(VersionConflictException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Version Conflict");
        problemDetail.setType(URI.create("https://flashpad.dev/errors/version-conflict"));
        problemDetail.setProperty("currentVersion", ex.getCurrentVersion());
        problemDetail.setProperty("currentContent", ex.getCurrentContent());
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Concurrent modification conflict: the pad was updated by another transaction"
        );
        problemDetail.setTitle("Version Conflict");
        problemDetail.setType(URI.create("https://flashpad.dev/errors/version-conflict"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://flashpad.dev/errors/validation-error"));

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://flashpad.dev/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
