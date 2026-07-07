package dev.scx.http.routing.x.static_files;

import dev.scx.http.exception.NotFoundException;
import dev.scx.http.routing.RoutingContext;
import dev.scx.http.routing.x.static_files.cache_control.CacheControl;
import dev.scx.http.uri.ScxURI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static dev.scx.http.headers.HttpHeaderName.CACHE_CONTROL;
import static dev.scx.http.headers.HttpHeaderName.LOCATION;
import static dev.scx.http.method.HttpMethod.GET;
import static dev.scx.http.method.HttpMethod.HEAD;
import static dev.scx.http.status_code.HttpStatusCode.PERMANENT_REDIRECT;

/// StaticFilesHandlerImpl
///
/// @author scx567888
public final class StaticFilesHandlerImpl implements StaticFilesHandler {

    private final Path root;
    private CacheControl cacheControl;

    public StaticFilesHandlerImpl(Path root) {
        // 转换为 绝对路径并归一化, 保证后续判断稳定
        this.root = root.toAbsolutePath().normalize();
        this.cacheControl = null;// 默认 null
    }

    /// 将捕获转换为 相对路径
    private static Path restToRelativePath(String rest) {
        if (rest == null) {
            // 没有 "*" 捕获 (模板未使用通配符), 视为错误
            throw new IllegalStateException("StaticFilesHandler requires wildcard capture '*'");
        }
        // 移除 全部前导 "/"
        var i = 0;
        while (i < rest.length() && rest.charAt(i) == '/') {
            i = i + 1;
        }
        return Path.of(rest.substring(i));
    }

    /// 如果 返回 null 表示 没有对应的文件.
    private static ResolveResult resolveFile(Path target, String rest) {
        // 1, 读取文件信息
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            // 无法读取目标文件属性时, 按 NOT_FOUND 处理.
            return null;
        }

        // 2, 如果是常规文件, 直接返回.
        if (attr.isRegularFile()) {
            return new ResolveResult(Type.FILE, target, attr);
        }

        // 3, 如果是目录.
        if (attr.isDirectory()) {

            // 这里对 尾部非 "/" 的路径进行重定向. 防止资源加载错误.
            var hasTrailingSlash = rest.endsWith("/");

            if (!hasTrailingSlash) {
                return new ResolveResult(Type.REDIRECT_TO_SLASH, null, null);
            }

            var indexTarget = target.resolve("index.html");

            BasicFileAttributes indexAttr;
            try {
                indexAttr = Files.readAttributes(indexTarget, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                // 无法读取目标文件属性时, 按 NOT_FOUND 处理.
                return null;
            }

            if (indexAttr.isRegularFile()) {
                return new ResolveResult(Type.FILE, indexTarget, indexAttr);
            }

        }

        return null;
    }

    @Override
    public void apply(RoutingContext context) throws Throwable {
        var request = context.request();
        var response = request.response();

        // 0, 只关心 GET 和 HEAD
        if (request.method() != GET && request.method() != HEAD) {
            context.next();
            return;
        }

        // 1, 获取捕获
        var rest = context.pathMatch().capture("*");

        // 2, 捕获转相对路径.
        var relativePath = restToRelativePath(rest);

        // 3, 这里 target 一定是绝对路径
        var target = root.resolve(relativePath).normalize();

        // 4, 校验 target 是否越界
        if (!target.startsWith(root)) {
            // 这里属于恶意输入 我们不 next 而是直接 抛出 404.
            throw new NotFoundException();
        }

        // 5, 读取真正需要发送的文件
        var resolveResult = resolveFile(target, rest);

        // 6, 文件不存在.
        if (resolveResult == null) {
            context.next();
            return;
        }

        // 7, 重定向.
        if (resolveResult.type() == Type.REDIRECT_TO_SLASH) {
            response.statusCode(PERMANENT_REDIRECT);
            var path = ScxURI.of(request.uri()).path(request.path() + "/");
            response.setHeader(LOCATION, path.encode(true));
            response.send();
            return;
        }

        // 8, 设置 cacheControl
        if (cacheControl != null) {
            response.setHeader(CACHE_CONTROL, cacheControl.encode());
        }

        // 9, 发送文件.
        StaticFilesSupport.serveFile(resolveResult.path().toFile(), resolveResult.attr(), request);

    }

    @Override
    public StaticFilesHandler cacheControl(CacheControl cacheControl) {
        this.cacheControl = cacheControl;
        return this;
    }

    public enum Type {
        FILE, REDIRECT_TO_SLASH
    }

    public record ResolveResult(Type type, Path path, BasicFileAttributes attr) {

    }

}
