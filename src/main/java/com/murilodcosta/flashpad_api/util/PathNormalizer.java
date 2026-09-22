package com.murilodcosta.flashpad_api.util;

public class PathNormalizer {

    private PathNormalizer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Normalizes a given path by:
     * - Returning "/" for null, empty or blank strings
     * - Replacing backslashes with hyphens
     * - Converting to lowercase
     * - Removing any characters that are not alphanumeric, hyphens, or slashes
     * - Stripping leading and trailing slashes and hyphens
     * - Collapsing multiple consecutive slashes into a single slash
     * - Collapsing multiple consecutive hyphens into a single hyphen
     * - Ensuring the resulting path starts with a leading slash
     */
    public static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        // Replace backslashes with hyphens
        path = path.replace("\\", "-");

        // Convert to lowercase for case-insensitivity
        path = path.toLowerCase();

        // Remove any characters that are not alphanumeric, hyphens, or slashes
        path = path.replaceAll("[^a-z0-9\\-/]", "");

        // Remove leading and trailing slashes and hyphens
        path = path.replaceAll("^[\\-/]+", "").replaceAll("[\\-/]+$", "");

        // Replace multiple consecutive slashes with a single slash
        path = path.replaceAll("/{2,}", "/");

        // Collapse multiple consecutive hyphens
        path = path.replaceAll("-{2,}", "-");

        if (path.isEmpty()) {
            return "/";
        }

        // Ensure the path starts with a single slash
        return "/" + path;
    }
}
