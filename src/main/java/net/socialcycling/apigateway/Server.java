package net.socialcycling.apigateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class Server extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) {

        Router router = Router.router(vertx);

        JWTAuthHandler authHandler = JWTAuthHandler.create(JWTAuth.create(vertx,
                new JsonObject().put("public-key", config().getString("keycloak.realm-public-key"))));

        router.route("/protected/*").order(-1).handler(authHandler);
        router.route("/protected/message").handler(this::handleHello);

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

    private void handleHello(RoutingContext routingContext) {
        JsonObject principal = routingContext.user().principal();
        routingContext
                .response()
                .putHeader("content-type", "application/json")
                .end(
                        new JsonObject()
                                .put("from", "vert.x")
                                .put("msg", "hello, " + principal.getString("name"))
                                .put("principal", principal)
                                .encodePrettily()
                );
    }
}
