package edu.kalum.api.kalum.management.core.verticles;

import edu.kalum.logging.core.application.dtos.CustomErrorDTO;
import edu.kalum.logging.core.helpers.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.UUID;

@Component
public class ProducerEnrollmentVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ProducerEnrollmentVerticle.class);
    private RabbitMQClient rabbitMQClient;
    @Autowired
    private Utils utils;

    @Override
    public void start() {
        RabbitMQOptions options = new RabbitMQOptions()
                .setUser(config().getJsonObject("rabbit").getString("user"))
                .setPassword(config().getJsonObject("rabbit").getString("password"))
                .setHost(config().getJsonObject("rabbit").getString("host"))
                .setPort(config().getJsonObject("rabbit").getInteger("port"))
                .setVirtualHost(config().getJsonObject("rabbit").getString("virtualHost"))
                .setAutomaticRecoveryEnabled(true);
        this.rabbitMQClient = RabbitMQClient.create(vertx, options);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route(HttpMethod.POST, "/kalum-management/v1/enrollments").handler(TimeoutHandler.create(5000, 504)).handler(ctx -> {
            Long initialTime = new Date().getTime();
            JsonObject order = ctx.body().asJsonObject();
            ctx.response().putHeader("Content-Type", "application/json");
            String id = UUID.randomUUID().toString();
            JsonObject message = new JsonObject()
                    .put("orderId", id)
                    .put("orderDate", utils.getDateWithFormat(new Date()))
                    .put("status", "IN_PROGRESS")
                    .put("data", order);
            sendOrder(message).onSuccess(messageResult -> {
                this.utils.log(initialTime,messageResult.encode(),202,"info","eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0","/kalum-management/v1/enrollments");
                ctx.response().setStatusCode(202);
                ctx.response().end(messageResult.encode());
            }).onFailure(error -> {
                CustomErrorDTO customError = this.utils.getCustonError(403,"Error al momento de generar la orden");
                this.utils.log(initialTime,error.getMessage(),403,"error","eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0","/kalum-management/v1/enrollments");
                ctx.response().setStatusCode(403);
                ctx.response().end(JsonObject.mapFrom(customError).encode());
            });
        });
        vertx.createHttpServer().requestHandler(router).listen(config().getJsonObject("server").getInteger("port")).onSuccess(http -> {
            logger.debug("El servidor HTTP ha iniciado correctamente en el puerto 9081");
        }).onFailure(error -> {
            this.utils.log(new Date().getTime(),"[API-KALUM-MANAGEMENT] ".concat(error.getMessage()),500,"error","eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0","/kalum-management/v1/enrollments");
        });
    }

    private Future<JsonObject> sendOrder(JsonObject order) {
        return rabbitMQClient.start().compose(startResult ->
                rabbitMQClient.basicPublish(config().getJsonObject("rabbit").getString("exchange"), config().getJsonObject("rabbit").getString("routerKey"), order.toBuffer())
        ).map(publishResult -> {
            return new JsonObject()
                    .put("orderId", order.getString("orderId"))
                    .put("orderDate", order.getString("orderDate"))
                    .put("message", "La orden fue creada exitosamente con el id ".concat(order.getString("orderId")))
                    .put("status", order.getString("status"));
        }).onFailure(error -> {
            this.utils.log(new Date().getTime(),"[RABBIT] ".concat(error.getMessage()),503,"error","eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0","/kalum-management/v1/enrollments");
        });
    }
}
