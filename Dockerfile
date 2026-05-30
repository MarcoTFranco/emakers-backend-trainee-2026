FROM eclipse-temurin:25-jdk-alpine
LABEL maintainer="Marco Túlio"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]