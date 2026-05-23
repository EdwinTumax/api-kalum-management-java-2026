package edu.kalum.api.kalum.management.core.verticles;

import edu.kalum.api.kalum.management.core.utilities.Utils;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
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
import java.util.Objects;
import java.util.UUID;

@Component
public class ProducerEnrollmentVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ProducerEnrollmentVerticle.class);

    private EventBus eventBus;
    private CircuitBreaker circuitBreaker;
    private RabbitMQClient rabbitMQClient;
    @Autowired
    private Utils utils;

    @Override
    public void start() {
        RabbitMQOptions options = new RabbitMQOptions()
                .setUser("guest")
                .setPassword("guest")
                .setHost("localhost")
                .setPort(5672)
                .setVirtualHost("/")
                .setAutomaticRecoveryEnabled(true);
        this.rabbitMQClient = RabbitMQClient.create(vertx, options);

        this.eventBus = vertx.eventBus();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route(HttpMethod.POST, "/kalum-management/v1/enrollments").handler(TimeoutHandler.create(5000, 504)).handler(ctx -> {
            JsonObject order = ctx.body().asJsonObject();
            ctx.response().putHeader("Content-Type", "application/json");
            String id = UUID.randomUUID().toString();
            JsonObject message = new JsonObject()
                    .put("orderId", id)
                    .put("orderDate", utils.getDateWithFormat(new Date()))
                    .put("status", "IN_PROGRESS")
                    .put("data", order);
            logger.info(message.encodePrettily());
            sendOrder(message).onSuccess(messageResult -> {
                ctx.response().setStatusCode(201);
                ctx.response().end(messageResult.encode());
            }).onFailure(error -> {
                ctx.response().setStatusCode(405);
                ctx.response().end("Proceso no completado");
            });
        });

        vertx.createHttpServer().requestHandler(router).listen(9081).onSuccess(http -> {
            logger.info("El servidor HTTP ha iniciado correctamente en el puerto 9081");
        }).onFailure(error -> {
            error.printStackTrace();
        });
    }

    private Future<JsonObject> sendOrder(JsonObject order) {
        return rabbitMQClient.start().compose(startResult ->
                rabbitMQClient.basicPublish("edu.kalum.exchange.order", "provide", order.toBuffer())
        ).map(publishResult -> {
            return new JsonObject()
                    .put("orderId", order.getString("orderId"))
                    .put("orderDate", order.getString("orderDate"))
                    .put("message", "La orden fue creada exitosamente con el id ".concat(order.getString("orderId")))
                    .put("status", order.getString("status"));
        }).onFailure(error -> {
            logger.error(error.getMessage());
        });
    }
}
