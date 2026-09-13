FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache libreoffice font-dejavu libwebp-tools \
    && addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

RUN mkdir -p /app/storage \
    && chown -R spring:spring /app /home/spring

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
