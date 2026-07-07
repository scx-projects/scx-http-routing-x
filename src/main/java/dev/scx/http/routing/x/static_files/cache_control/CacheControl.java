package dev.scx.http.routing.x.static_files.cache_control;

import java.util.LinkedHashSet;

/// CacheControl
///
/// @author scx567888
public final class CacheControl {

    private final String value;

    private CacheControl(String... directives) {
        if (directives == null) {
            throw new NullPointerException("directives must not be null");
        }
        if (directives.length == 0) {
            throw new IllegalArgumentException("directives must not be empty");
        }

        var set = new LinkedHashSet<String>();

        for (var d : directives) {
            if (d == null) {
                throw new NullPointerException("directive must not be null");
            }

            String trimmed = d.trim();

            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("directive must not be blank");
            }

            if (trimmed.contains(",")) {
                throw new IllegalArgumentException(
                    "directive must not contain ',' : " + trimmed);
            }

            set.add(trimmed);
        }

        this.value = String.join(", ", set);
    }

    public static CacheControl of(String... directives) {
        return new CacheControl(directives);
    }

    public String encode() {
        return value;
    }

}
