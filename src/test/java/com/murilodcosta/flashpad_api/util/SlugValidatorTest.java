package com.murilodcosta.flashpad_api.util;

import com.murilodcosta.flashpad_api.exception.InvalidSlugException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugValidatorTest {

    @Test
    @DisplayName("Should validate and return simple valid slug")
    void shouldValidateSimpleSlug() {
        String result = SlugValidator.validate("my-note");
        assertThat(result).isEqualTo("my-note");
    }

    @Test
    @DisplayName("Should validate single character slug")
    void shouldValidateSingleCharSlug() {
        assertThat(SlugValidator.validate("a")).isEqualTo("a");
        assertThat(SlugValidator.validate("9")).isEqualTo("9");
    }

    @Test
    @DisplayName("Should normalize slug with spaces, uppercase, underscores and extra hyphens")
    void shouldNormalizeSlug() {
        assertThat(SlugValidator.validate("  Hello World  ")).isEqualTo("hello-world");
        assertThat(SlugValidator.validate("my_note_name")).isEqualTo("my-note-name");
        assertThat(SlugValidator.validate("note---with---hyphens")).isEqualTo("note-with-hyphens");
        assertThat(SlugValidator.validate("-trimmed-hyphens-")).isEqualTo("trimmed-hyphens");
    }

    @Test
    @DisplayName("Should validate exactly 64 characters slug")
    void shouldValidateMax64CharsSlug() {
        String exact64Chars = "a".repeat(64);
        assertThat(SlugValidator.validate(exact64Chars)).isEqualTo(exact64Chars);
    }

    @Test
    @DisplayName("Should reject slug exceeding 64 characters")
    void shouldRejectSlugExceeding64Chars() {
        String oversized = "a".repeat(65);
        assertThatThrownBy(() -> SlugValidator.validate(oversized))
                .isInstanceOf(InvalidSlugException.class)
                .hasMessageContaining("Slugs must contain only lowercase alphanumeric characters and hyphens (up to 64 characters)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"api", "actuator", "assets", "r", "admin", "static", "health"})
    @DisplayName("Should reject reserved slug keywords")
    void shouldRejectReservedKeywords(String reserved) {
        assertThatThrownBy(() -> SlugValidator.validate(reserved))
                .isInstanceOf(InvalidSlugException.class)
                .hasMessage("The slug '" + reserved + "' is reserved and cannot be used.");
    }

    @Test
    @DisplayName("Should reject reserved slug even when provided with uppercase and spaces")
    void shouldRejectNormalizedReservedKeyword() {
        assertThatThrownBy(() -> SlugValidator.validate("  ACTUATOR  "))
                .isInstanceOf(InvalidSlugException.class)
                .hasMessage("The slug 'actuator' is reserved and cannot be used.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid/path", "note@123", "hello.world", "coração", "pad#tag", "pad!alert"})
    @DisplayName("Should reject slugs with special or forbidden characters")
    void shouldRejectSpecialCharacters(String invalidSlug) {
        assertThatThrownBy(() -> SlugValidator.validate(invalidSlug))
                .isInstanceOf(InvalidSlugException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "---", "--"})
    @DisplayName("Should reject blank or hyphen-only slugs")
    void shouldRejectBlankOrHyphenOnlySlugs(String blankSlug) {
        assertThatThrownBy(() -> SlugValidator.validate(blankSlug))
                .isInstanceOf(InvalidSlugException.class);
    }

    @Test
    @DisplayName("Should reject null slug")
    void shouldRejectNullSlug() {
        assertThatThrownBy(() -> SlugValidator.validate(null))
                .isInstanceOf(InvalidSlugException.class);
    }

    @Test
    @DisplayName("isValid should return true for valid and false for invalid")
    void shouldReturnCorrectBooleanForIsValid() {
        assertThat(SlugValidator.isValid("valid-slug")).isTrue();
        assertThat(SlugValidator.isValid("api")).isFalse();
        assertThat(SlugValidator.isValid("invalid/slug")).isFalse();
        assertThat(SlugValidator.isValid("")).isFalse();
        assertThat(SlugValidator.isValid(null)).isFalse();
    }
}
