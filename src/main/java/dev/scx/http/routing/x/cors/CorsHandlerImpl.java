package dev.scx.http.routing.x.cors;

import dev.scx.http.method.HttpMethod;
import dev.scx.http.routing.RoutingContext;
import dev.scx.http.routing.x.cors.allow_headers.AllowHeaders;
import dev.scx.http.routing.x.cors.allow_methods.AllowMethods;
import dev.scx.http.routing.x.cors.allow_origin.AllowOrigin;
import dev.scx.http.routing.x.cors.expose_headers.ExposeHeaders;

import static dev.scx.http.headers.HttpHeaderName.*;

/// CorsHandlerImpl
///
/// @author scx567888
final class CorsHandlerImpl implements CorsHandler {

    private AllowOrigin allowOrigin;
    private AllowMethods allowMethods;
    private AllowHeaders allowHeaders;
    private ExposeHeaders exposeHeaders;
    private boolean allowCredentials;
    private Long maxAgeSeconds;

    public CorsHandlerImpl() {
        this.allowOrigin = AllowOrigin.ofReflect();
        this.allowMethods = AllowMethods.ofReflect();
        this.allowHeaders = AllowHeaders.ofReflect();
        this.exposeHeaders = ExposeHeaders.ofNone();
        this.allowCredentials = true;
        this.maxAgeSeconds = null;
    }

    @Override
    public CorsHandler allowOrigin(AllowOrigin allowOrigin) {
        if (allowOrigin == null) {
            throw new NullPointerException("allowOrigin must not be null");
        }
        this.allowOrigin = allowOrigin;
        return this;
    }

    @Override
    public CorsHandler allowMethods(AllowMethods allowMethods) {
        if (allowMethods == null) {
            throw new NullPointerException("allowMethods must not be null");
        }
        this.allowMethods = allowMethods;
        return this;
    }

    @Override
    public CorsHandler allowHeaders(AllowHeaders allowHeaders) {
        if (allowHeaders == null) {
            throw new NullPointerException("allowHeaders must not be null");
        }
        this.allowHeaders = allowHeaders;
        return this;
    }

    @Override
    public CorsHandler exposeHeaders(ExposeHeaders exposeHeaders) {
        if (exposeHeaders == null) {
            throw new NullPointerException("exposeHeaders must not be null");
        }
        this.exposeHeaders = exposeHeaders;
        return this;
    }

    @Override
    public CorsHandlerImpl allowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
        return this;
    }

    @Override
    public CorsHandlerImpl maxAgeSeconds(Long maxAgeSeconds) {
        if (maxAgeSeconds != null && maxAgeSeconds < 0) {
            throw new IllegalArgumentException("maxAgeSeconds must >= 0");
        }
        this.maxAgeSeconds = maxAgeSeconds;
        return this;
    }

    @Override
    public void apply(RoutingContext context) throws Throwable {
        var request = context.request();
        var response = request.response();

        var origin = request.getHeader(ORIGIN);

        // 1, 校验是否是 cors 请求.
        if (origin == null) {
            // 不是 CORS 请求 - 什么都不做 直接 next
            context.next();
            return;
        }

        // 2, 验证 origin
        var allowedOrigin = this.allowOrigin.allowedOrigin(origin);

        // 验证失败
        if (allowedOrigin == null) {
            context.next();
            return;
        }

        // 3, 验证成功
        // 3.1, 写入 Allow-Origin
        response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        response.setHeader(VARY, "Origin");

        // 3.2, 写入 Allow-Credentials
        if (allowCredentials) {
            response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        // 4, 判断是否是预检请求
        var requestMethod = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);

        // 4.1, 是预检请求
        if (request.method() == HttpMethod.OPTIONS && requestMethod != null) {

            // 这里 allowedMethods 永不为 null
            // 因为 ListAllowMethods 和 WildcardAllowMethods 都不会返回 null
            // 而 ReflcetAllowMethods 只有在 requestMethod == null 时才会返回 null
            // 但是我们的分支判断已经杜绝了这种可能性.
            var allowedMethods = this.allowMethods.allowedMethods(requestMethod);
            response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);

            var requestHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);

            // 这里 和上边的 allowMethods 判断类似,
            // 只有 ReflectAllowHeaders 并且 requestHeaders == null 才会返回 null
            // 而这 在协议中是允许的 表示 不设置 "Access-Control-Allow-Headers" 头.
            var allowedHeaders = this.allowHeaders.allowedHeaders(requestHeaders);
            if (allowedHeaders != null) {
                response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
            }

            if (this.maxAgeSeconds != null) {
                response.setHeader(ACCESS_CONTROL_MAX_AGE, String.valueOf(maxAgeSeconds));
            }

            response.statusCode(204).send();

        } else { // 不是预检请求

            var exposedHeaders = this.exposeHeaders.exposedHeaders();
            if (exposedHeaders != null) {
                response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
            }

            context.next();
        }

    }

}
