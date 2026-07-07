package dev.scx.http.routing.x.single_file;

import dev.scx.http.routing.RoutingContext;
import dev.scx.http.routing.x.static_files.StaticFilesSupport;
import dev.scx.http.routing.x.static_files.cache_control.CacheControl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static dev.scx.http.headers.HttpHeaderName.CACHE_CONTROL;
import static dev.scx.http.method.HttpMethod.GET;
import static dev.scx.http.method.HttpMethod.HEAD;

/// SingleFileHandlerImpl
///
/// @author scx567888
public final class SingleFileHandlerImpl implements SingleFileHandler {

    private final Path file;
    private CacheControl cacheControl;

    public SingleFileHandlerImpl(Path file) {
        // 转换为 绝对路径并归一化, 保证后续判断稳定
        this.file = file.toAbsolutePath().normalize();
    }

    /// 如果 返回 null 表示 没有对应的文件.
    private static ResolveResult resolveFile(Path target) {
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
            return new ResolveResult(target, attr);
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

        // 1, 读取真正需要发送的文件
        var resolveResult = resolveFile(file);

        // 2, 文件不存在.
        if (resolveResult == null) {
            context.next();
            return;
        }

        // 3, 设置 cacheControl
        if (cacheControl != null) {
            response.setHeader(CACHE_CONTROL, cacheControl.encode());
        }

        // 4, 发送文件.
        StaticFilesSupport.serveFile(resolveResult.path().toFile(), resolveResult.attr(), request);

    }

    @Override
    public SingleFileHandler cacheControl(CacheControl cacheControl) {
        this.cacheControl = cacheControl;
        return this;
    }

    public record ResolveResult(Path path, BasicFileAttributes attr) {

    }

}
