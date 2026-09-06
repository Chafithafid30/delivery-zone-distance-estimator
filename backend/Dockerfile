FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp verify

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl && addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/delivery-zone-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
