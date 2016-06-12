package net.socialcycling.apigateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class Server extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) {

        Router router = Router.router(vertx);

        router.route("/").handler(routingContext -> {
            routingContext
                    .response()
                    .putHeader("content-type", "application/json")
                    .end(
                            new JsonObject()
                            .put("from", "vert.x")
                            .put("msg", "hello, socialcyling")
                            .encodePrettily()
                    );
        });

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port", 7175),
                        result -> {
                            if (result.succeeded()) {
                                startFuture.complete();
                            } else {
                                startFuture.fail(result.cause());
                            }
                        }
                );
    }
}
