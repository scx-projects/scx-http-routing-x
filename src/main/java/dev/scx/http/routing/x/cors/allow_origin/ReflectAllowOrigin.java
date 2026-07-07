package dev.scx.http.routing.x.cors.allow_origin;

/// ReflectAllowOrigin
///
/// @author scx567888
public final class ReflectAllowOrigin implements AllowOrigin {

    public static final ReflectAllowOrigin REFLECT_ALLOW_ORIGIN = new ReflectAllowOrigin();

    /// 保证单例
    private ReflectAllowOrigin() {

    }

    @Override
    public String allowedOrigin(String origin) {
        return origin;
    }

}
