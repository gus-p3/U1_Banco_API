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

# Crear grupo y usuario de sistema no privilegiados (UID/GID 1001) para Alpine Linux
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copiar el archivo JAR asignando directamente la propiedad al usuario no root
COPY --from=build --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# Permisos estrictos de lectura y ejecución mínima para la app
RUN chmod 500 app.jar && chown -R appuser:appgroup /app

# Configurar variables de entorno y puerto por defecto
ENV PORT=8080
EXPOSE 8080

# Ejecutar el contenedor como usuario no privilegiado (cierra dockerfile.security.missing-user-entrypoint)
USER 1001:1001

# Comando de arranque
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
