package com.murilodcosta.flashpad_api.dto;

public record PadUpdateEvent(
        String content,
        Long version,
        String clientId
) {
}
