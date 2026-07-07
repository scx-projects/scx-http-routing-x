package dev.scx.http.routing.x.cors.allow_origin;

import static dev.scx.http.routing.x.cors.allow_origin.ReflectAllowOrigin.REFLECT_ALLOW_ORIGIN;
import static dev.scx.http.routing.x.cors.allow_origin.WildcardAllowOrigin.WILDCARD_ALLOW_ORIGIN;

/// AllowOrigin
///
/// @author scx567888
public interface AllowOrigin {

    static ListAllowOrigin of(String... origins) {
        return new ListAllowOrigin(origins);
    }

    static ReflectAllowOrigin ofReflect() {
        return REFLECT_ALLOW_ORIGIN;
    }

    static WildcardAllowOrigin ofWildcard() {
        return WILDCARD_ALLOW_ORIGIN;
    }

    String allowedOrigin(String origin);

}
