package dev.scx.http.routing.x.static_files;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.exception.NotFoundException;
import dev.scx.http.routing.x.static_files.content_range.ContentRange;
import dev.scx.http.routing.x.static_files.http_date.HttpDateHelper;
import dev.scx.http.routing.x.static_files.http_validator.HttpValidator;
import dev.scx.http.routing.x.static_files.range.IllegalRangeException;
import dev.scx.http.routing.x.static_files.range.Range;
import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.sender.ScxHttpReceiveException;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.io.exception.ScxOutputException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.temporal.ChronoUnit;

import static dev.scx.http.headers.HttpHeaderName.*;
import static dev.scx.http.status_code.HttpStatusCode.*;

/// StaticFilesSupport
///
/// 提供与静态文件响应相关的基础能力.
///
/// `sendFile(...)` 用于根据当前 HTTP 请求语义发送一个文件,
/// 并处理常见的 HTTP 行为 (如 Range).
///
/// 该方法仅依赖 `ScxHttpServerRequest`, 因此并不要求在 routing
/// 环境中使用. 除了 `StaticFilesHandler` 外, 用户也可以在
/// 手写 handler 或其它场景中直接调用.
///
/// 例如:
///
/// ```java
/// StaticFilesSupport.sendFile(file, request);
/// ```
///
/// 常见使用场景:
/// - 文件下载接口
/// - FSS (file system service)
/// - 自定义 handler 中返回文件
///
/// @author scx567888
public final class StaticFilesSupport {

    /// 这里关于 416 响应 我们直接采用标准响应方式而不是 异常方式.
    public static void sendFile(File target, BasicFileAttributes attr, ScxHttpServerRequest request) throws NotFoundException, IllegalSenderStateException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        var response = request.response();

        // 0, 如果不是常规文件, 一律 404, 这里使用 异常方式, 因为本质上这就是异常.
        if (!attr.isRegularFile()) {
            throw new NotFoundException();
        }

        // 1, 让客户端知道我们支持分段加载
        response.setHeader(ACCEPT_RANGES, "bytes");

        // 2, 判断是不是 Range 请求
        var rangeStr = request.getHeader(RANGE);

        // 3, 不是 Range 请求, 发送完整文件.
        if (rangeStr == null) {
            // 浏览器在视频拖动/跳转时, 可能主动中断连接.
            // 由于无法精确区分写出异常的来源, 这里对写出阶段的输出异常统一降噪处理.
            try {
                response.send(target);
            } catch (ScxWrappedException e) {
                if (e.getCause() instanceof ScxOutputException) {
                    return;
                }
                // 其余正常抛出
                throw e;
            }

            return;
        }

        var fileSize = attr.size();

        // 4, 是 Range 请求, 判断请求是否合法(包括范围是否合法).
        Range range;
        try {
            range = Range.parse(rangeStr);
        } catch (IllegalRangeException e) {
            // Range 解析失败 416.
            response.statusCode(RANGE_NOT_SATISFIABLE);
            response.setHeader(CONTENT_RANGE, ContentRange.ofUnsatisfied(fileSize).encode());
            response.send();
            return;
        }

        var contentRange = resolveContentRange(range, fileSize);

        if (contentRange.isUnsatisfied()) {
            // 范围不合法 416.
            response.statusCode(RANGE_NOT_SATISFIABLE);
            response.setHeader(CONTENT_RANGE, contentRange.encode());
            response.send();
            return;
        }

        // 5, 返回 206 响应.
        response.statusCode(PARTIAL_CONTENT);
        response.setHeader(CONTENT_RANGE, contentRange.encode());
        var offset = contentRange.start();
        var length = contentRange.end() - contentRange.start() + 1;

        // 浏览器在视频拖动/跳转时, 可能主动中断连接.
        // 由于无法精确区分写出异常的来源, 这里对写出阶段的输出异常统一降噪处理.
        try {
            response.send(target, offset, length);
        } catch (ScxWrappedException e) {
            if (e.getCause() instanceof ScxOutputException) {
                return;
            }
            // 其余正常抛出
            throw e;
        }

    }

    /// serveFile
    public static void serveFile(File target, BasicFileAttributes attr, ScxHttpServerRequest request) throws NotFoundException, IllegalSenderStateException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        var response = request.response();
        var validator = createValidator(attr);

        var notModified = checkNotModified(validator, request);

        response.setHeader(ETAG, validator.etag());
        response.setHeader(LAST_MODIFIED, HttpDateHelper.encode(validator.lastModified()));

        if (notModified) {
            response.statusCode(NOT_MODIFIED).send();
        } else {
            StaticFilesSupport.sendFile(target, attr, request);
        }
    }

    public static ContentRange resolveContentRange(Range range, long size) {

        if (size == 0) {
            return ContentRange.ofUnsatisfied(size);
        }

        var start = range.start();
        var end = range.end();

        // bytes=start-end
        if (start != null && end != null) {

            if (start >= size) {
                return ContentRange.ofUnsatisfied(size);
            }

            long realEnd = Math.min(end, size - 1);

            return ContentRange.of(start, realEnd, size);
        }

        // bytes=start-
        if (start != null) {

            if (start >= size) {
                return ContentRange.ofUnsatisfied(size);
            }

            return ContentRange.of(start, size - 1, size);
        }

        // bytes=-suffix
        long suffix = end;

        if (suffix == 0) {
            return ContentRange.ofUnsatisfied(size);
        }

        long realStart = Math.max(size - suffix, 0);
        long realEnd = size - 1;
        return ContentRange.of(realStart, realEnd, size);

    }

    public static HttpValidator createValidator(BasicFileAttributes attr) {
        long size = attr.size();
        var lastModifiedTime = attr.lastModifiedTime();

        var etag = "\"" + size + "-" + lastModifiedTime.toMillis() + "\"";
        var lastModified = lastModifiedTime.toInstant().truncatedTo(ChronoUnit.SECONDS);
        return new HttpValidator(etag, lastModified);
    }

    public static boolean checkNotModified(HttpValidator httpValidator, ScxHttpServerRequest request) {

        var ifNoneMatch = request.getHeader(IF_NONE_MATCH);
        var ifModifiedSince = request.getHeader(IF_MODIFIED_SINCE);

        if (ifNoneMatch != null) {
            return ifNoneMatch.equals(httpValidator.etag());
        }

        if (ifModifiedSince != null) {

            try {
                var ims = HttpDateHelper.parse(ifModifiedSince);
                return !httpValidator.lastModified().isAfter(ims);
            } catch (Exception ignored) {
                // 解析 ifModifiedSince 失败我们认为没有有效的 ifModifiedSince, 返回 false
                return false;
            }
        }

        return false;
    }

    // ******************** 便捷方法 *************************

    public static void sendFile(File target, ScxHttpServerRequest request) throws NotFoundException, IllegalSenderStateException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        // 读取文件信息
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(target.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            // 无法读取目标文件属性时, 按 NOT_FOUND 处理.
            throw new NotFoundException();
        }
        sendFile(target, attr, request);
    }

    public static void serveFile(File target, ScxHttpServerRequest request) throws NotFoundException, IllegalSenderStateException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        // 读取文件信息
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(target.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            // 无法读取目标文件属性时, 按 NOT_FOUND 处理.
            throw new NotFoundException();
        }
        serveFile(target, attr, request);
    }

}
