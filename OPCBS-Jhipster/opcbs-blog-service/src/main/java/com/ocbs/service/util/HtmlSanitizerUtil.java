package com.ocbs.service.util;

import java.util.regex.Pattern;

/**
 * Utility for sanitizing HTML in Blog Posts to prevent XSS (Cross-Site Scripting).
 */
public final class HtmlSanitizerUtil {

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script[^>]*?>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IFRAME_TAG_PATTERN = Pattern.compile("<iframe[^>]*?>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OBJECT_TAG_PATTERN = Pattern.compile("<object[^>]*?>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EMBED_TAG_PATTERN = Pattern.compile("<embed[^>]*?>.*?</embed>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern APPLET_TAG_PATTERN = Pattern.compile("<applet[^>]*?>.*?</applet>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern STYLE_TAG_PATTERN = Pattern.compile("<style[^>]*?>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_TAG_PATTERN = Pattern.compile("<meta[^>]*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LINK_TAG_PATTERN = Pattern.compile("<link[^>]*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FORM_TAG_PATTERN = Pattern.compile("</?form[^>]*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BASE_TAG_PATTERN = Pattern.compile("<base[^>]*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // Matches any inline event handler such as onload=..., onerror=..., onclick=..., etc.
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("(?i)\\s+on[a-zA-Z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");

    // Matches javascript:, vbscript:, data: protocols inside attributes
    private static final Pattern DANGEROUS_PROTOCOLS = Pattern.compile("(?i)(href|src)\\s*=\\s*(\"|')\\s*(javascript|vbscript|data):.*\\2");

    private static final Pattern ALL_HTML_TAGS = Pattern.compile("<[^>]*>");

    private HtmlSanitizerUtil() {}

    /**
     * Sanitizes blog HTML content by removing executable scripts, frames, event handlers, and active payloads.
     */
    public static String sanitizeContent(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String cleaned = input;
        cleaned = SCRIPT_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = IFRAME_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = OBJECT_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EMBED_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = APPLET_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = STYLE_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = META_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = LINK_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = FORM_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = BASE_TAG_PATTERN.matcher(cleaned).replaceAll("");

        cleaned = EVENT_HANDLER_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = DANGEROUS_PROTOCOLS.matcher(cleaned).replaceAll("$1=\"#\"");

        return cleaned;
    }

    /**
     * Strips all HTML tags entirely for plain text fields like title, author name, or excerpt.
     */
    public static String stripAllHtml(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return ALL_HTML_TAGS.matcher(input).replaceAll("").trim();
    }
}
