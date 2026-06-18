# Etapa 1: compilar el JAR con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: imagen liviana solo con el JRE
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/apirest-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
# Railway hobby ~512MB: heap acotado + SerialGC (menos overhead que G1)
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms96m -Xmx320m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.address=0.0.0.0 --server.port=${PORT:-8080}"]
