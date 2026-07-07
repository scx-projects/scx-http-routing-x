package dev.scx.http.routing.x.static_files.http_validator;

import java.time.Instant;

/// HttpValidator
///
/// @author scx567888
public record HttpValidator(String etag, Instant lastModified) {

}
