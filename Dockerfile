# Etapa 1: Compilación con JDK 21
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar archivos del Gradle Wrapper
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Otorgar permisos y precargar dependencias para aprovechar la caché de Docker
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# Copiar código fuente y compilar el JAR ejecutable
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Etapa 2: Imagen de ejecución ligera con JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el archivo JAR generado desde la etapa de compilación
COPY --from=build /app/build/libs/*.jar app.jar

# Configurar variables de entorno y puerto por defecto
ENV PORT=8080
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
