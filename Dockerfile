FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY admin-web/package*.json admin-web/

RUN mvn dependency:go-offline

COPY src src
COPY admin-web/ admin-web/

RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-alpine
WORKDIR /app

COPY --from=builder /app/target/bot-for-consultations-0.0.1-SNAPSHOT.jar app.jar

# Указываем порт, который будет слушать наше приложение
EXPOSE 8080

# Команда для запуска приложения при старте контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]