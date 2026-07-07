package dev.scx.http.routing.x.cors.allow_methods;

/// ReflectAllowMethods
///
/// @author scx567888
public final class ReflectAllowMethods implements AllowMethods {

    public static final ReflectAllowMethods REFLECT_ALLOW_METHODS = new ReflectAllowMethods();

    /// 保证单例
    private ReflectAllowMethods() {

    }

    @Override
    public String allowedMethods(String requestMethodString) {
        return requestMethodString;
    }

}
