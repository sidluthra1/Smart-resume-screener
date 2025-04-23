// --------------------------------------------------------------
// utils/CitationCleaner.java
// --------------------------------------------------------------
package com.yourname.backend.util;

public class CitationCleaner {

    // matches:  :contentReference[oaicite:2]{index=2}   (or any other UI element)
    private static final String UI_TAG_REGEX = "[\uE200].*?[\uE201]";

    /** remove any ChatGPT UI-element markers from a string */
    public static String strip(String s) {
        return s == null ? null : s.replaceAll(UI_TAG_REGEX, "").trim();
    }
}
