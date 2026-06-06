FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/api-kalum-management-1.0.0.jar api-kalum-management-1.0.0.jar
RUN mkdir -p /var/log/app
ENTRYPOINT ["sh", "-c", "APP_HOSTNAME=$HOSTNAME APP_CLIENT_IP=$(hostname -i) APP_VERSION=1 exec java -jar api-kalum-management-1.0.0.jar"]
EXPOSE 9080