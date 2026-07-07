package dev.scx.http.routing.x.cors.allow_origin;

/// WildcardAllowOrigin
///
/// @author scx567888
public final class WildcardAllowOrigin implements AllowOrigin {

    public static final WildcardAllowOrigin WILDCARD_ALLOW_ORIGIN = new WildcardAllowOrigin();

    /// 保证单例
    private WildcardAllowOrigin() {

    }

    @Override
    public String allowedOrigin(String origin) {
        return "*";
    }

}
