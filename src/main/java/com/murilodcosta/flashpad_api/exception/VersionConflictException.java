package com.murilodcosta.flashpad_api.exception;

import lombok.Getter;

@Getter
public class VersionConflictException extends RuntimeException {
    private final Long currentVersion;
    private final String currentContent;

    public VersionConflictException(Long currentVersion, String currentContent) {
        super("Version conflict: the pad has been modified by another request");
        this.currentVersion = currentVersion;
        this.currentContent = currentContent;
    }
}
