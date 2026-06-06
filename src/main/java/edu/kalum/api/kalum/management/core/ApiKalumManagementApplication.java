package edu.kalum.api.kalum.management.core;

import edu.kalum.api.kalum.management.core.verticles.ProducerEnrollmentVerticle;
import edu.kalum.logging.core.helpers.Utils;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.Date;

@SpringBootApplication
public class ApiKalumManagementApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ApiKalumManagementApplication.class);

	@Autowired
	private ProducerEnrollmentVerticle producerEnrollmentVerticle;

	@Autowired
	private Environment env;

	@Autowired
	private Utils utils;

	public static void main(String[] args) {
		SpringApplication.run(ApiKalumManagementApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		String envConfig = env.getProperty("SPRING_PROFILES_ACTIVE") != null ? env.getProperty("SPRING_PROFILES_ACTIVE") : "env";
		ConfigStoreOptions configStoreOptionsEnv = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path",envConfig.concat(".json")));
		ConfigStoreOptions configStoreOptionsSys = new ConfigStoreOptions().setType("sys");
		ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions().addStore(configStoreOptionsEnv).addStore(configStoreOptionsSys);
		ConfigRetriever configRetriever = ConfigRetriever.create(Vertx.vertx(),configRetrieverOptions);

		configRetriever.getConfig().onSuccess(config -> {
			Vertx.vertx().deployVerticle(producerEnrollmentVerticle, new DeploymentOptions().setConfig(config)).onSuccess(id -> {
				logger.debug("Deployment ok: ".concat(id));
			});
		}).onFailure(error -> {
			this.utils.log(new Date().getTime(),error.getMessage(),503,"error","eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0","/kalum-management/v1/enrollments");
		});
	}
}
