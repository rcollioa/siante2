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
# Heap como porcentaje del límite de memoria del contenedor (resources.limits.memory en k8s): con el
# límite de 1024Mi da ~768 MB y deja el resto para metaspace, hilos y buffers. Sin esto la JVM toma
# solo el 25% (256 MB). Va en JAVA_TOOL_OPTIONS porque la JVM la lee sola; es el valor por defecto
# cuando el ambiente no define JAVA_OPTS.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# Usuario sin privilegios, con uid numérico fijo para que k8s pueda verificarlo si se usa runAsNonRoot.
# La aplicación solo lee app.jar (queda de root, sin permiso de escritura para la app) y escribe en
# /tmp (Tomcat embebido), que es escribible por cualquier usuario.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin app
USER 10001
EXPOSE 8080
# JAVA_OPTS (en k8s, del ConfigMap de kustomization.yml) se pasa a la JVM y manda sobre el valor por defecto de arriba: un
# -Xmx explícito reemplaza a MaxRAMPercentage. `exec` deja a Java como proceso 1 para que reciba el
# SIGTERM de k8s y se detenga ordenadamente.
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
