package com.murilodcosta.flashpad_api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PathNormalizerTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should return root slash for null, empty or blank paths")
    void shouldReturnRootSlashForNullOrBlankPaths(String input) {
        String result = PathNormalizer.normalize(input);
        assertThat(result).isEqualTo("/");
    }

    @Test
    @DisplayName("Should ensure leading slash and remove trailing slash")
    void shouldNormalizeLeadingAndTrailingSlashes() {
        assertThat(PathNormalizer.normalize("notes")).isEqualTo("/notes");
        assertThat(PathNormalizer.normalize("/notes")).isEqualTo("/notes");
        assertThat(PathNormalizer.normalize("/notes/")).isEqualTo("/notes");
        assertThat(PathNormalizer.normalize("notes/")).isEqualTo("/notes");
    }

    @Test
    @DisplayName("Should collapse consecutive slashes into single slash")
    void shouldCollapseConsecutiveSlashes() {
        assertThat(PathNormalizer.normalize("///notes///today///")).isEqualTo("/notes/today");
        assertThat(PathNormalizer.normalize("a//b///c")).isEqualTo("/a/b/c");
    }

    @Test
    @DisplayName("Should convert uppercase characters to lowercase")
    void shouldConvertToLowercase() {
        assertThat(PathNormalizer.normalize("MyNotes")).isEqualTo("/mynotes");
        assertThat(PathNormalizer.normalize("/WORKSPACE/PROJECT-A")).isEqualTo("/workspace/project-a");
    }

    @Test
    @DisplayName("Should replace backslashes with hyphens")
    void shouldReplaceBackslashesWithHyphens() {
        assertThat(PathNormalizer.normalize("folder\\subfolder")).isEqualTo("/folder-subfolder");
        assertThat(PathNormalizer.normalize("\\my\\note")).isEqualTo("/my-note");
    }

    @Test
    @DisplayName("Should strip invalid special characters")
    void shouldStripSpecialCharacters() {
        assertThat(PathNormalizer.normalize("note!@#$%^&*()+=")).isEqualTo("/note");
        assertThat(PathNormalizer.normalize("/path?query=1&test=2")).isEqualTo("/pathquery1test2");
    }

    @Test
    @DisplayName("Should preserve valid hyphens and numbers")
    void shouldPreserveHyphensAndNumbers() {
        assertThat(PathNormalizer.normalize("note-123-v2")).isEqualTo("/note-123-v2");
        assertThat(PathNormalizer.normalize("/docs/chapter-1/page-42")).isEqualTo("/docs/chapter-1/page-42");
    }
}
