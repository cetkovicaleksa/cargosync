FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
ARG MVN_ARGS="-DskipTests -Pproduction"
RUN mvn clean package --batch-mode ${MVN_ARGS}

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
ARG UPLOADS_DIR=uploads
RUN mkdir -p ${UPLOADS_DIR}
ENV CARGOSYNC_CONTENT_ROOT=${WORKDIR}/${UPLOADS_DIR}
ARG JAR_FILE=/app/target/*.jar
COPY --from=builder ${JAR_FILE} /app/app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD nc -z localhost 8080 || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD []
