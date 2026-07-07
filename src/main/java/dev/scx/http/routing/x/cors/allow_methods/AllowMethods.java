package dev.scx.http.routing.x.cors.allow_methods;

import dev.scx.http.method.ScxHttpMethod;

import static dev.scx.http.routing.x.cors.allow_methods.ReflectAllowMethods.REFLECT_ALLOW_METHODS;
import static dev.scx.http.routing.x.cors.allow_methods.WildcardAllowMethods.WILDCARD_ALLOW_METHODS;

/// AllowMethods
///
/// @author scx567888
public interface AllowMethods {

    static ListAllowMethods of(ScxHttpMethod... methods) {
        return new ListAllowMethods(methods);
    }

    static ReflectAllowMethods ofReflect() {
        return REFLECT_ALLOW_METHODS;
    }

    static WildcardAllowMethods ofWildcard() {
        return WILDCARD_ALLOW_METHODS;
    }

    String allowedMethods(String requestMethodString);

}
