# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copia os arquivos do Maven Wrapper e o pom.xml primeiro para aproveitar o cache de camadas.
# Se o pom.xml não mudar, o Docker reutiliza a camada de dependências sem baixar novamente.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw
RUN ./mvnw dependency:go-offline -q

# Copia o código-fonte e compila o JAR (sem rodar testes — banco não está disponível aqui)
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine
LABEL maintainer="Marco Túlio"
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
