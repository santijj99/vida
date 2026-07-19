# Etapa 1: compilar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline -B

COPY src ./src
RUN mvn -q clean package -DskipTests -B

# Etapa 2: runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring \
    && useradd -r -g spring spring

COPY --from=build /app/target/apirest-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/data/afip && chown -R spring:spring /app

USER spring:spring

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENV PORT=3000
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]
