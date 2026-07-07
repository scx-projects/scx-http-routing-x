package dev.scx.http.routing.x.cors;

import dev.scx.function.Function1Void;
import dev.scx.http.routing.RoutingContext;
import dev.scx.http.routing.x.cors.allow_headers.AllowHeaders;
import dev.scx.http.routing.x.cors.allow_methods.AllowMethods;
import dev.scx.http.routing.x.cors.allow_origin.AllowOrigin;
import dev.scx.http.routing.x.cors.expose_headers.ExposeHeaders;

/// CorsHandler
///
/// @author scx567888
public interface CorsHandler extends Function1Void<RoutingContext, Throwable> {

    /// 创建一个默认配置的 CORS 处理器.
    ///
    /// 默认配置为:
    /// - allowOrigin = reflect
    /// - allowMethods = reflect
    /// - allowHeaders = reflect
    /// - exposeHeaders = none
    /// - allowCredentials = true
    /// - maxAgeSeconds = null
    static CorsHandler of() {
        return new CorsHandlerImpl();
    }

    CorsHandler allowOrigin(AllowOrigin allowOrigin);

    CorsHandler allowMethods(AllowMethods allowMethods);

    CorsHandler allowHeaders(AllowHeaders allowHeaders);

    CorsHandler exposeHeaders(ExposeHeaders exposeHeaders);

    CorsHandler allowCredentials(boolean allowCredentials);

    CorsHandler maxAgeSeconds(Long maxAgeSeconds);

}
