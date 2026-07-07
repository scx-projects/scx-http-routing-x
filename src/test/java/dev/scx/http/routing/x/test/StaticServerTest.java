package dev.scx.http.routing.x.test;

import dev.scx.http.routing.Router;
import dev.scx.http.routing.x.cors.CorsHandler;
import dev.scx.http.routing.x.single_file.SingleFileHandler;
import dev.scx.http.routing.x.static_files.StaticFilesHandler;
import dev.scx.http.routing.x.static_files.StaticFilesSupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class StaticServerTest {

    public static void main(String[] args) throws IOException {
        test1();
    }

    public static void test1() throws IOException {
        Router router = Router.of();

        // 尽可能靠前
        router.route(-10000, CorsHandler.of());

        router.route("/hello", ctx -> {
            ctx.request().response().send("hello");
        });

        router.route("/download", ctx -> {
            File file = new File("xxx");
            StaticFilesSupport.sendFile(file, ctx.request());
        });

        router.route("/image", ctx -> {
            File file = new File("xxx");
            StaticFilesSupport.serveFile(file, ctx.request());
        });

        // 标准静态服务器模式
        router.route("/*", StaticFilesHandler.of(Path.of("XXXX")));

        // spa 模式
        router.route("/*", SingleFileHandler.of(Path.of("XXX.html")));

    }

}
