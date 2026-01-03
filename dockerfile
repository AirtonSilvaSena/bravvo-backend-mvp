# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia tudo e compila
COPY . .
RUN mvn clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia o jar gerado
COPY --from=build /app/target/*.jar app.jar

# Porta (Render usa PORT)
EXPOSE 8080

# Start
ENTRYPOINT ["java", "-jar", "app.jar"]
