package com.murilodcosta.flashpad_api.util;

import com.murilodcosta.flashpad_api.exception.InvalidSlugException;

import java.util.Set;
import java.util.regex.Pattern;

public final class SlugValidator {

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$");

    private static final Set<String> RESERVED_SLUGS =
            Set.of("api", "actuator", "assets", "r", "admin", "static", "health");

    private SlugValidator() {
        // Prevent instantiation
    }

    /**
     * Normalizes a raw slug:
     * - Trims whitespace
     * - Converts to lowercase
     * - Replaces spaces and underscores with hyphens
     * - Collapses consecutive hyphens
     * - Strips leading and trailing hyphens
     */
    public static String normalize(String rawSlug) {
        if (rawSlug == null) {
            return "";
        }
        String normalized = rawSlug.trim().toLowerCase();
        normalized = normalized.replace('_', '-').replace(' ', '-');
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        return normalized;
    }

    /**
     * Validates whether a slug conforms to length (1 to 64 chars), character rules,
     * and is not among reserved system keywords.
     *
     * @param rawSlug the raw slug provided by the user
     * @return the normalized valid slug
     * @throws InvalidSlugException if the slug is invalid or reserved
     */
    public static String validate(String rawSlug) {
        String slug = normalize(rawSlug);

        if (slug.isEmpty() || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new InvalidSlugException(
                    "Invalid slug: '" + (rawSlug != null ? rawSlug : "") +
                    "'. Slugs must contain only lowercase alphanumeric characters and hyphens (up to 64 characters), and cannot start or end with a hyphen."
            );
        }

        if (RESERVED_SLUGS.contains(slug)) {
            throw new InvalidSlugException("The slug '" + slug + "' is reserved and cannot be used.");
        }

        return slug;
    }

    /**
     * Checks if a raw slug is valid without throwing an exception.
     */
    public static boolean isValid(String rawSlug) {
        String slug = normalize(rawSlug);
        return !slug.isEmpty() && SLUG_PATTERN.matcher(slug).matches() && !RESERVED_SLUGS.contains(slug);
    }
}
