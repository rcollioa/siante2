# SIANTE (siante2)

Aplicación web de la Contraloría General de la República, creada a partir de la plantilla de proyecto derivada de Geoportal Municipal. Trae la arquitectura, la infraestructura transversal y el despliegue listos; los módulos de negocio se agregan en `features/` (frontend) y en las capas del backend (ver "Agregar un módulo").

## Resumen

- backend: API REST en Java 25 con Spring Boot 4.1, arquitectura hexagonal y JPA/Hibernate sobre Oracle. Se empaqueta como JAR ejecutable.
- frontend: aplicación Angular 22 con componentes standalone y signals.
- Despliegue: una imagen Docker por aplicación y manifiestos de Kubernetes para dev y prod.

Qué trae ya resuelto:

| Área | Backend | Frontend |
|---|---|---|
| Prefijo de la API | `ApiPrefijoConfig` (`app.api.prefijo`) | `API_PREFIJO`, `esRutaApi` |
| Errores | `ApiExceptionHandler`: todo error responde `{code, message}`. 400 `INVALID_REQUEST` (también validación, parámetros y JSON mal formado), 503 `DATA_SOURCE_UNAVAILABLE`, 404/405 y 500 con el nombre del estado | `reintentoInterceptor` (502/503/504), `tiempoLimiteInterceptor` (30 s), `tolerarFallos` |
| Caché | `CacheControlAdvice` (`app.cache.max-age`) | `CacheConsultas` en memoria, `?refrescar` en la URL |
| CORS | `CorsConfig` (`APP_CORS_ALLOWED_ORIGINS`) | — |
| Carga | — | Pantalla de carga global automática (`pantallaCargaInterceptor`) |
| Salud | Actuator: `/actuator/health/liveness` y `/readiness` | nginx: `/health` |
| Endurecimiento | Imagen JRE, usuario 10001; en k8s, disco de solo lectura, sin privilegios ni capabilities | nginx sin root; CSP y cabeceras de seguridad (`nginx-cabeceras-seguridad.conf`); fuentes servidas desde la app, sin Google Fonts |
| Dependencias | Dependabot semanal (`.github/dependabot.yml`) | Dependabot semanal |
| Calidad | Tests con `@WebMvcTest`, contexto completo contra H2, JPQL compilado en el build | Vitest, ESLint con reglas de accesibilidad |

## Nombre del proyecto

El nombre técnico es `siante2`, igual en todas partes (paquete Java, URLs, Kubernetes, DNS e imágenes). El título visible es "SIANTE".

| Qué | Dónde | Valor |
|---|---|---|
| Prefijo de la API | `backend/src/main/resources/application.properties` y `src/test/resources/application.properties` (`app.api.prefijo`), `ApiPrefijoConfig`, `CorsConfig`, `frontend/src/app/shared/api/api-prefijo.ts`, `frontend/src/proxy.conf*.json`, `k8s/*/ingress.yml`, tests de `adapter/in/web` | `/siante2/api` |
| Ruta pública del frontend | `frontend/angular.json` (`baseHref`), `frontend/src/index.html`, `frontend/nginx.conf`, `frontend/Dockerfile` | `/siante2/` |
| Paquete Java | `backend/src/main/java/siante2`, `backend/src/test/java/siante2` y `ConsultasJpqlTest` | `siante2` |
| Título | `frontend/src/index.html`, `AppComponent.titulo` | `SIANTE` |
| Kubernetes | `k8s/*/**`: namespace, nombres, host, imagen, ConfigMap y secreto | `siante2-test`, `siante2-prod`, `siante2.dev-qa.tanzu.contraloria.cl`, `siante2.tanzu.contraloria.cl`, `ghcr.io/cgr-chile/siante2-*` |
| Contenedores locales | `docker-compose.yml` | `siante2-backend`, `siante2-frontend` |

El prefijo de la API debe quedar igual en backend, frontend, proxies e ingress: si no coinciden, el frontend recibe 404.

## Estructura

```text
backend/src/main/java/siante2/
  BackendApplication.java
  domain/                      SolicitudInvalidaException; model/: records de dominio
  application/
    port/in/                   un caso de uso por interfaz (ObtenerXUseCase)
    port/out/                  lo que los servicios necesitan leer (XReadPort)
    service/                   implementan port/in y usan port/out
  adapter/
    in/web/                    @RestController y configuración web transversal
    out/oracle/                implementan port/out con Spring Data JPA
      entity/                  @Entity
      repository/              JpaRepository
frontend/src/app/
  app.component.*              shell: cabecera, router-outlet, pantalla de carga
  app.routes.ts
  features/inicio/             página de inicio (reemplazar)
  shared/                      api, cache, errores, pantalla-carga
k8s/dev, k8s/prod              kustomize: backend, frontend, ingress, secreto
```

Cada paquete del backend tiene un `package-info.java` que describe qué va en él.

## Agregar un módulo

Ejemplo para un módulo `informes`:

1. Dominio: `domain/model/Informe.java` como `record`, sin Spring ni JPA.
2. Puerto de entrada: `application/port/in/ObtenerInformesUseCase.java`.
3. Puerto de salida: `application/port/out/InformesReadPort.java`.
4. Servicio: `application/service/InformesService.java`, que implementa el caso de uso, valida la entrada lanzando `SolicitudInvalidaException` y llama al puerto de salida.
5. Adaptador Oracle: `adapter/out/oracle/entity/InformeEntity.java`, `adapter/out/oracle/repository/InformeJpaRepository.java` y `adapter/out/oracle/OracleInformesRepository.java`, que implementa `InformesReadPort` y traduce entidades a modelos de dominio.
6. Controlador: `adapter/in/web/InformesController.java` con `@RequestMapping("/informes")`, sin el prefijo de la API.
7. Tests: el servicio con un doble en memoria del puerto de salida, y el controlador con `@WebMvcTest` y `@MockitoBean` del caso de uso.
8. Frontend: `features/informes/informes-api.service.ts`, que usa `API_PREFIJO` y `CacheConsultas`, más el componente, registrado en `app.routes.ts`.

Las entidades y los repositorios se detectan solos por estar bajo el paquete de `BackendApplication`. Si el proyecto necesita una segunda base, cada una necesita su propio `DataSource`, `EntityManagerFactory` y `@EnableJpaRepositories` (Geoportal Municipal tiene un ejemplo con GEOCGR y SIAPER).

## Stack y versiones

- Java: JDK 25 LTS. Maven 3.9.16 mediante el wrapper incluido (`mvnw`).
- Spring Boot: 4.1, con Tomcat embebido.
- Node.js: 22.x LTS (`frontend/.nvmrc`), npm 10.x.
- Angular: 22.

## Inicio rápido

### Backend

Definir las variables de conexión (`APP_DATASOURCE_URL`, `APP_DATASOURCE_USERNAME`, `APP_DATASOURCE_PASSWORD`), descritas en `backend/src/main/resources/application.properties`. No guardar credenciales en el repositorio. Sin base de datos el backend no arranca; los tests sí corren sin ella.

En PowerShell (ajustar la ruta del JDK si es necesario):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.3'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd backend
.\mvnw.cmd spring-boot:run
```

En Linux o macOS, usar `./mvnw` con `JAVA_HOME` apuntando a un JDK 25. La API queda en `http://localhost:8080/siante2/api/`.

### Frontend

```bash
cd frontend
npm ci
npm start
```

La aplicación queda en `http://localhost:4200/siante2/`. El proxy de desarrollo (`src/proxy.conf.json`) redirige `/siante2/api` al backend en `http://localhost:8080`.

### Con Docker

Copiar `.env.example` como `.env`, completarlo y ejecutar `docker compose up --build` desde la raíz.

## Tests

```bash
cd backend
./mvnw test
```

- `ConfiguracionWebTest` y `CorsConfigTest` prueban prefijo, caché, errores y CORS sobre `ControladorPrueba`, un controlador que solo existe en `src/test`.
- `ContextoAplicacionTest` levanta el contexto completo contra H2 en memoria (`src/test/resources/application.properties`).
- `ConsultasJpqlTest` compila cada `@Query` JPQL de `adapter/out/oracle/repository` contra el modelo de entidades, sin conectarse a Oracle. Sin repositorios, no genera casos.

```bash
cd frontend
npm test
npm run lint
```

Los dos Dockerfile corren los tests (y el del frontend, también el lint) antes del build: si alguno falla, la imagen no se construye.

## Despliegue

Mismo flujo GitOps que Geoportal Municipal:

1. Se hace push a `develop`.
2. Un pipeline externo construye las imágenes con `backend/Dockerfile` y `frontend/Dockerfile` y las publica en `ghcr.io`.
3. Argo CD sincroniza los manifiestos de `k8s/`.

| Ambiente | Manifiestos | Namespace | Réplicas |
|---|---|---|---|
| Dev/QA | `k8s/dev` | `siante2-test` | 1 de cada aplicación |
| Producción | `k8s/prod` | `siante2-prod` | 2, con rolling update sin indisponibilidad |

El ingress envía `/siante2/api` al backend y el resto al frontend.

El secreto real (`k8s/*/secret.yml`) lo define Ingeniería y está ignorado por git. `secret.example.yml` lista las credenciales que espera el backend. `kustomization.yml` referencia `secret.yml`, así que debe existir antes de aplicar los manifiestos.

La configuración que no es secreta (`TZ`, `APP_CORS_ALLOWED_ORIGINS`, `JAVA_OPTS`) está en `configMapGenerator` de `k8s/*/kustomization.yml`. Kustomize agrega un hash del contenido al nombre del ConfigMap, así que cambiar un valor reinicia los pods del backend sin pasos manuales. Con `JAVA_OPTS` vacío, el heap queda en el 75 % de `resources.limits.memory` (ver `backend/Dockerfile`).

### Versión de las imágenes

Los deployments no fijan etiqueta: la pone `images[].newTag` de `k8s/*/kustomization.yml`. Debe ser una etiqueta inmutable, el SHA del commit que construyó la imagen, nunca `:dev` ni `:prod`. Así se sabe qué versión corre en cada ambiente, las réplicas de prod no pueden quedar con imágenes distintas, y volver a una versión anterior es revertir un commit.

1. El pipeline publica `ghcr.io/cgr-chile/siante2-backend:<sha>` y `ghcr.io/cgr-chile/siante2-frontend:<sha>`.
2. Actualiza `newTag` en `k8s/dev/kustomization.yml` con un commit (por ejemplo, con `kustomize edit set image ghcr.io/cgr-chile/siante2-backend:<sha>`). Argo CD despliega al ver el cambio.
3. Para pasar a producción no se reconstruye: se copia a `k8s/prod/kustomization.yml` el mismo `newTag` validado en dev.

El valor inicial `REEMPLAZAR_POR_SHA` no corresponde a ninguna imagen: el primer despliegue falla hasta que el pipeline lo reemplace.
