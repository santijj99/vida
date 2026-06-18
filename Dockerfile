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

ENV PORT=3000
EXPOSE 3000

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
