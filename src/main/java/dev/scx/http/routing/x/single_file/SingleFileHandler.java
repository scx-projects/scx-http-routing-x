package dev.scx.http.routing.x.single_file;

import dev.scx.function.Function1Void;
import dev.scx.http.routing.RoutingContext;
import dev.scx.http.routing.x.static_files.cache_control.CacheControl;

import java.nio.file.Path;

/// SingleFileHandler
///
/// @author scx567888
public interface SingleFileHandler extends Function1Void<RoutingContext, Throwable> {

    /// 创建一个固定文件处理器.
    ///
    /// 该处理器始终尝试发送给定 `file`,
    /// 通常用于精确路径映射;
    /// 也可作为回退路由的一部分, 与 `StaticFilesHandler` 组合使用,
    /// 用于实现 SPA 入口页分发.
    static SingleFileHandler of(Path file) {
        return new SingleFileHandlerImpl(file);
    }

    SingleFileHandler cacheControl(CacheControl cacheControl);

}
