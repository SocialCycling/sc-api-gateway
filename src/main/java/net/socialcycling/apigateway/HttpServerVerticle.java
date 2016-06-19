package net.socialcycling.apigateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;

public class HttpServerVerticle extends AbstractVerticle {

    private ServiceDiscovery discovery;


    @Override
    public void start(Future<Void> startFuture) {

        discovery = ServiceDiscovery.create(vertx);

        Router router = Router.router(vertx);

        JWTAuthHandler authHandler = JWTAuthHandler.create(JWTAuth.create(vertx,
                new JsonObject().put("public-key", config().getString("keycloak.realm-public-key"))));

        router.get("/geocode").handler(authHandler);
        router.get("/geocode").handler(this::handleGeocode);

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port"),
                        result -> {
                            if (result.succeeded()) {
                                startFuture.complete();
                            } else {
                                startFuture.fail(result.cause());
                            }
                        }
                );
    }

    @Override
    public void stop() throws Exception {
        if (discovery != null) {
            discovery.close();
        }
    }


    private void handleGeocode(RoutingContext ctx) {
        Double eps = config().getDouble("geocoding.viewbox.eps");
        String query = "/search.php?viewbox="
                + (Double.parseDouble(ctx.request().getParam("longitude")) - eps) + ","
                + (Double.parseDouble(ctx.request().getParam("latitude")) - eps) + ","
                + (Double.parseDouble(ctx.request().getParam("longitude")) + eps) + ","
                + (Double.parseDouble(ctx.request().getParam("latitude")) + eps) + "&"
                + "q=" + ctx.request().getParam("query") + "&"
                + "format=" + config().getString("geocoding.format") + "&"
                + "bounded=" + config().getInteger("geocoding.bounded") + "&"
                + "limit=" + config().getInteger("geocoding.limit");
        HttpServerResponse serverResponse = ctx.response();
        HttpEndpoint.getClient(
                discovery,
                new JsonObject().put("name", "net.socialcycling.geocoding"),
                geocoderDiscoveryResult -> {
                    if (geocoderDiscoveryResult.succeeded()) {
                        HttpClient geocoderClient = geocoderDiscoveryResult.result();
                        geocoderClient.getNow(query, clientResponse -> {
                            serverResponse
                                    .putHeader("content-type", "application/json")
                                    .setChunked(true);
                            Pump.pump(clientResponse, serverResponse).start();
                            clientResponse.endHandler(v -> {
                                serverResponse.end();
                                ServiceDiscovery.releaseServiceObject(discovery, geocoderClient);
                            });
                        });
                    } else  {
                        serverResponse
                                .setStatusCode(500)
                                .setStatusMessage("geocoding service unavailable")
                                .end();
                    }
                }
        );
    }

}
