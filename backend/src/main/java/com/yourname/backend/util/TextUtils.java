package com.yourname.backend.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TextUtils {
    private TextUtils() {}                 // utility class

    public static List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Stream.of(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
