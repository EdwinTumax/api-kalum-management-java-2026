package edu.kalum.api.kalum.management.core;

import edu.kalum.api.kalum.management.core.verticles.ProducerEnrollmentVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiKalumManagementApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ApiKalumManagementApplication.class);

	@Autowired
	private ProducerEnrollmentVerticle producerEnrollmentVerticle;

	public static void main(String[] args) {
		SpringApplication.run(ApiKalumManagementApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Vertx.vertx().deployVerticle(producerEnrollmentVerticle).onSuccess(id -> {
			logger.info("Deployment ok: ".concat(id));
		}).onFailure(error -> {
			error.printStackTrace();
		});
	}
}
