# Multi-stage build: compila con la imagen que ya trae Maven (sin mvnw, que en buildah no puede descomprimir Maven)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
# Con tests: si alguno falla, la imagen no se construye. Ninguno necesita Oracle (controladores con
# @WebMvcTest, contexto completo contra H2 en memoria), así que corren igual dentro del build.
RUN mvn -q clean package

# Solo el runtime (JRE), sin compilador ni herramientas del JDK: imagen más chica y con menos que
# atacar si alguien llega a ejecutar código en el contenedor.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
# Heap al 75 % del límite de memoria del contenedor (la JVM toma 25 % por defecto).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# Usuario sin privilegios con uid fijo (runAsNonRoot); la app solo lee app.jar y escribe en /tmp.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin app
USER 10001
EXPOSE 8080
# JAVA_OPTS (ConfigMap en k8s) manda sobre MaxRAMPercentage.
# `exec` deja a Java como proceso 1 para que reciba el SIGTERM de k8s.
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
