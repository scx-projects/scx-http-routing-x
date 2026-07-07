package dev.scx.http.routing.x.static_files;

import dev.scx.function.Function1Void;
import dev.scx.http.routing.RoutingContext;
import dev.scx.http.routing.x.static_files.cache_control.CacheControl;

import java.nio.file.Path;

/// StaticFilesHandler
///
/// @author scx567888
public interface StaticFilesHandler extends Function1Void<RoutingContext, Throwable> {

    /// 创建一个目录型静态文件处理器.
    ///
    /// 该处理器依赖当前路由提供 `*` 捕获,
    /// 应挂载在 `/*`、`/assets/*` 等带尾部通配段的模板下.
    /// 若当前路由未提供 `*` 捕获, 运行时会抛出 `IllegalStateException`.
    static StaticFilesHandler of(Path root) {
        return new StaticFilesHandlerImpl(root);
    }

    StaticFilesHandler cacheControl(CacheControl cacheControl);

}
