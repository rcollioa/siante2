# SIATE (siante2)

Aplicación GIS de la Contraloría General de la República: nueva versión de SIATE (capas, grupos, categorías, análisis espacial, reportes públicos con QR y exportación). Cada módulo de negocio se agrega en `features/` (frontend) y en su propio paquete del backend (ver "Agregar un módulo").

## Resumen

- backend: API REST en Java 25 con Spring Boot 4.1, arquitectura hexagonal y JPA/Hibernate sobre Oracle. Se empaqueta como JAR ejecutable.
- frontend: aplicación Angular 22 con componentes standalone y signals.
- Despliegue: una imagen Docker por aplicación y manifiestos de Kubernetes para dev y prod.

Qué trae ya resuelto:

| Área | Backend | Frontend |
|---|---|---|
| Prefijo de la API | `ApiPrefijoConfig` (`app.api.prefijo`) | `API_PREFIJO`, `esRutaApi` |
| Errores | `ApiExceptionHandler`: todo error responde `{code, message}`. 400 `INVALID_REQUEST` (también validación, parámetros y JSON mal formado), 404 `NOT_FOUND` (`RecursoNoEncontradoException`), 503 `EXTERNAL_SERVICE_UNAVAILABLE` (`ServicioExternoNoDisponibleException`), 503 `DATA_SOURCE_UNAVAILABLE`, 404/405 y 500 con el nombre del estado | `reintentoInterceptor` (502/503/504, salvo `EXTERNAL_SERVICE_UNAVAILABLE`: el backend ya reintentó al tercero), `tiempoLimiteInterceptor` (30 s), `tolerarFallos` |
| Caché | `CacheControlAdvice`: siempre `private` (ningún proxy guarda datos de un usuario); los controladores `@Catalogo` se guardan en el navegador `app.cache.max-age` y el resto se revalida (`no-cache` + ETag de `EtagApiFilter`, 304 si no cambió). Caffeine con `@Cacheable` para catálogos (`categorias`, `multimedia`), vigencia `SIANTE_CACHE_SERVIDOR` (1 h por defecto) | `CacheConsultas` en memoria, `?refrescar` en la URL |
| CORS | `CorsConfig` (`APP_CORS_ALLOWED_ORIGINS`) | — |
| Carga | — | Pantalla de carga global automática (`pantallaCargaInterceptor`) |
| Salud | Actuator en su propio puerto (8081, `MANAGEMENT_SERVER_PORT`): `/actuator/health/liveness` y `/readiness`. Solo lo usan las sondas de k8s; ni el Service ni el ingress lo publican | nginx: `/health` |
| Endurecimiento | Imagen JRE, usuario 10001 sin privilegios | nginx sin root; CSP y cabeceras de seguridad (`nginx-cabeceras-seguridad.conf`); fuentes servidas desde la app, sin Google Fonts |
| Dependencias | Dependabot semanal (`.github/dependabot.yml`) | Dependabot semanal |
| Calidad | Tests con `@WebMvcTest`, contexto completo contra H2, JPQL compilado en el build, contrato OpenAPI versionado | Vitest, ESLint con reglas de accesibilidad |

## Nombre del proyecto

El nombre técnico es `siante2`, igual en todas partes (paquete Java, URLs, Kubernetes, DNS e imágenes). El título visible es "SIATE".

La diferencia es intencional: es la segunda versión del sistema cuyo nombre técnico siempre fue `siante` (repositorio, WAR y `/siante/api`). En octubre de 2026 se decidió no pasar el nombre técnico a `siate2`, porque implicaría cambiar URLs, DNS, imágenes, el secreto y el junction de WebSEAL, sin ningún beneficio para los usuarios. Los textos visibles siempre dicen SIATE.

| Qué | Dónde | Valor |
|---|---|---|
| Prefijo de la API | `backend/src/main/resources/application.properties` y `src/test/resources/application.properties` (`app.api.prefijo`), `ApiPrefijoConfig`, `CorsConfig`, `frontend/src/app/shared/api/api-prefijo.ts`, `frontend/src/proxy.conf*.json`, `k8s/*/ingress.yml`, tests de `compartido/adapter/in/web` y de cada controlador | `/siante2/api` |
| Ruta pública del frontend | `frontend/angular.json` (`baseHref`), `frontend/src/index.html`, `frontend/nginx.conf`, `frontend/Dockerfile` | `/siante2/` |
| Paquete Java | `backend/src/main/java/siante2`, `backend/src/test/java/siante2` y `ConsultasJpqlTest` | `siante2` |
| Título | `frontend/src/index.html` (`<title>`), texto alternativo del logo en `menu-lateral` y `no-autorizado` (`assets/img/logo-siate.svg`) | `SIATE` |
| Kubernetes | `k8s/*/**`: nombres, host, imagen y secreto (namespaces compartidos `sisgeob-test` y `sisgeob-prod`) | `siante2-*`, `siante2.dev-qa.tanzu.contraloria.cl`, `siante2.tanzu.contraloria.cl`, `ghcr.io/cgr-chile/siante2-*` |
| Contenedores locales | `docker-compose.yml` | `siante2-backend`, `siante2-frontend` |

El prefijo de la API debe quedar igual en backend, frontend, proxies e ingress: si no coinciden, el frontend recibe 404.

## Estructura

```text
backend/src/main/java/siante2/
  BackendApplication.java
  compartido/                  transversal: SolicitudInvalidaException, errores, prefijo, caché, CORS, OpenAPI
  <modulo>/                    un paquete por módulo (categorias, capas, grupos...), con la misma forma:
    domain/                    records de dominio, sin Spring ni JPA
    application/
      port/in/                 un caso de uso por interfaz (ListarXUseCase)
      port/out/                lo que los servicios necesitan leer (XReadPort)
      service/                 implementan port/in y usan port/out
    adapter/
      in/web/                  @RestController del módulo
      out/oracle/              @Entity, JpaRepository y la implementación de port/out
frontend/src/app/
  app.component.*              shell: cabecera, router-outlet, pantalla de carga
  app.routes.ts
  features/<modulo>/           con negocio o mapa, hexagonal como el backend (ver "Frontend hexagonal"):
    domain/                    tipos y reglas en TypeScript puro
    application/               casos de uso (estado con signals) y port/out/ (abstract class)
    adapter/in/ui/             componentes
    adapter/out/http|openlayers/  implementaciones de los puertos
    <modulo>.providers.ts      qué adaptador implementa cada puerto; se registra en main.ts
  features/mapa/               mapa base: MapaPort y su adaptador de OpenLayers (capas base, elementos, ubicación)
  features/categorias/         servicio de API de categorías
  features/ayuda/              políticas de uso, manual (PDF) y guías animadas desde /multimedia
  features/direcciones/        buscador de direcciones sobre el mapa (geocodificación en el backend)
  features/capas/              "Mis capas": lista en el menú, capa abierta en el mapa e información de sus elementos
  shared/api/contrato.ts       tipos generados desde el contrato OpenAPI (npm run api:tipos)
  shared/                      api, cache, errores, pantalla-carga
k8s/dev, k8s/prod              kustomize: backend, frontend, ingress, secreto
```

El backend se organiza por módulo: todo lo de un módulo queda en su paquete y se revisa junto en un PR. Solo lo transversal va en `compartido/`.

## Agregar un módulo

`categorias` es el módulo de referencia. Para un módulo `informes`, todo bajo `siante2/informes/`:

1. Dominio: `domain/Informe.java` como `record`, sin Spring ni JPA.
2. Puerto de entrada: `application/port/in/ListarInformesUseCase.java`.
3. Puerto de salida: `application/port/out/InformesReadPort.java`.
4. Servicio: `application/service/InformesService.java`, que implementa el caso de uso, valida la entrada lanzando `SolicitudInvalidaException` y llama al puerto de salida.
5. Adaptador Oracle en `adapter/out/oracle/`: `InformeEntity` (con `@Immutable` mientras SIATE escriba en la misma tabla), `InformeJpaRepository` y `OracleInformesRepository`, que implementa `InformesReadPort` y traduce entidades a modelos de dominio. Repositorio y adaptador sin `public`: nada fuera del paquete conoce JPA.
6. Controlador: `adapter/in/web/InformesController.java` con `@RequestMapping("/informes")`, sin el prefijo de la API. Los listados van envueltos en `{ "data": [...] }` y el nombre del método es el `operationId` del contrato (`listarInformes`, no `listar`). Solo si los datos son iguales para todos y casi no cambian, el controlador lleva `@Catalogo`; los datos de cada usuario o que se editan quedan sin anotación, así se revalidan y una edición se ve al instante.
7. Tests: el controlador con `@WebMvcTest` y `@MockitoBean` del caso de uso; el adaptador con `@DataJpaTest` sobre H2; el servicio con un doble en memoria del puerto de salida cuando tenga lógica.
8. Contrato: regenerar `contrato/openapi.json` (ver Tests) y revisar el diff.
9. Frontend: `npm run api:tipos` para regenerar `shared/api/contrato.ts` desde el contrato. Luego, en `features/informes/` (ver `features/capas` y "Frontend hexagonal"): el dominio, el puerto `InformesRepositorio`, el caso de uso, el adaptador `adapter/out/http/informes-http.ts` (con esos tipos, `API_PREFIJO` y, si es un catálogo, `CacheConsultas`), `informes.providers.ts` registrado en `main.ts` y el componente en `adapter/in/ui/`, registrado en `app.routes.ts`.

### Reglas de arquitectura

`ArquitecturaTest` (ArchUnit) las verifica en cada build; si una falla, la imagen no se construye:

- El dominio es Java puro: sin Spring, JPA, Hibernate ni Jackson (la forma JSON va en un `@JacksonMixin` de la capa web, como `GeometriaJsonMixin`).
- `application` no conoce adaptadores; los controladores (`adapter/in`) no usan repositorios ni entidades.
- Las entidades JPA viven y se usan solo en `adapter/out/oracle`; los repositorios de Spring Data no son `public`.
- Un módulo usa de otro solo su dominio y sus puertos; `compartido` lo usan todos y no conoce a ninguno; sin ciclos entre módulos.

### Frontend hexagonal

`mapa` y `capas` siguen la misma forma que el backend, y todo módulo nuevo con negocio o mapa también (favoritos, grupos, reportes). Los módulos sin reglas propias (`ayuda`, `direcciones`, `no-autorizado`, `salida`) siguen planos hasta que las tengan.

- **Puertos como `abstract class`**: en Angular sirven de interfaz y de token de inyección (`CapasRepositorio`, `MapaPort`). Cada módulo los conecta en `<modulo>.providers.ts` (`proveerCapas()`), que `main.ts` registra; las pruebas de integración usan la misma función.
- **El dominio no conoce el contrato**: el adaptador HTTP entrega los DTO como tipos del dominio, así que si el contrato deja de calzar no compila, y traduce los errores (un 404 es `CapaNoEncontrada`).
- **El mapa es un puerto**: `MapaPort` recibe elementos del dominio (`ElementoMapa`) y solo `mapa/adapter/out/openlayers` usa OpenLayers.
- **Casos de uso con signals de solo lectura**: el estado cambia solo por sus métodos (`abrir`, `cerrar`, `descartarError`). Se prueban con dobles en memoria de los puertos, sin HTTP ni mapa.
- **Componentes `OnPush`**, sin `standalone: true` (es el valor por defecto desde Angular 19).

ESLint (`no-restricted-imports` en `eslint.config.js`) verifica en cada build, y el Dockerfile corre el lint:

| Capa | No puede importar |
|---|---|
| `domain` | Angular, RxJS, OpenLayers, el contrato, `application` ni adaptadores |
| `application` | `@angular/common/http`, OpenLayers, el contrato ni adaptadores |
| `adapter/in` | adaptadores de salida, `@angular/common/http`, OpenLayers ni el contrato |
| cualquier otro archivo | adaptadores de salida (salvo `<modulo>.providers.ts`) |

### Prácticas de JPA / Hibernate (7.4, la versión la fija Spring Boot)

- Lecturas: proyección directa al dominio (`select new ...(…)` o métodos derivados) en vez de cargar entidades para mapearlas; `coalesce` en la consulta para que el dominio no reciba null.
- Transacciones: adaptadores con `@Transactional(readOnly = true)` a nivel de clase; métodos que escriben con `@Transactional`; el caso de uso que combina lectura y escritura, `@Transactional` en el servicio.
- Limitar en la base (`findFirstBy…OrderBy…`, paginación), nunca `.stream().findFirst()` sobre una lista.
- Asociaciones siempre `LAZY` (también `@ManyToOne`); para cargarlas, `join fetch` o `@EntityGraph` en la consulta que las necesita. Sin `FetchType.EAGER`.
- Entidades: atributos con el nombre de la columna en camelCase (`LAST_NAME` → `lastName`, `L_GROUP_ID` → `lGroupId`), sin `@Column`: los mapea la estrategia de nombres de Spring Boot. `@Column` solo cuando esa regla no da la columna (dos mayúsculas seguidas, p. ej. `PURE_X_PURE`). Clases con sufijo `Entity`; los nombres en español quedan en el dominio. `EntidadesSiateTest` verifica columna por columna.
- Entidades: `@Immutable` mientras SIATE escriba en la tabla, constructor `protected` vacío, sin setters, IDs por trigger con `IDENTITY`.
- Vistas (`V_*`): no tienen clave primaria y el `@Id` de su entidad se infiere del modelo de SIATE. Si se repitiera, Hibernate devolvería una instancia por clave y descartaría el resto sin aviso. Por eso se leen solo con proyecciones, desde un repositorio que extiende `Repository` y no `JpaRepository`, para que nadie use `findAll` ni `findById`.
- CLOB: `JSON_METADATA` de una capa llega a 1.800.000 caracteres (el límite de SIATE antes de usar `SCHEDULER_DATA`). Los listados no seleccionan columnas CLOB; se leen por elemento cuando se necesitan.
- Tests de repositorio verifican la cantidad de SQL con `ContadorSql` (estadísticas de Hibernate solo en el perfil `test`).
- Configuración global en `application.properties`: `jdbc.fetch_size=100` (Oracle trae 10 por defecto), lotes de 50, `default_batch_fetch_size`, `in_clause_parameter_padding`, `fail_on_pagination_over_collection_fetch` y `log_slow_query` (`SIANTE_LOG_CONSULTA_LENTA_MS`).

Las entidades y los repositorios se detectan solos por estar bajo el paquete de `BackendApplication`. Si el proyecto necesita una segunda base, cada una necesita su propio `DataSource`, `EntityManagerFactory` y `@EnableJpaRepositories`. Es lo que hará falta cuando un módulo use GEOCGR (`GEOCGR_DATASOURCE_*` en `.env`).

### Servicios externos

El navegador solo habla con la API propia (la CSP no permite otros orígenes en `connect-src`); los servicios externos se llaman desde un adaptador de salida (`adapter/out/<servicio>`), como `direcciones/adapter/out/nominatim`:

- Cliente `RestClient` con tiempo límite (connect y lectura) y el proxy de la JVM (`-Dhttps.proxyHost`, vía `JAVA_TOOL_OPTIONS`).
- `@Retryable` y `@ConcurrencyLimit` de Spring Framework 7 (`ResilienciaConfig`), más `@Cacheable` para no repetir consultas.
- Si falla, el adaptador lanza `ServicioExternoNoDisponibleException` y la API responde 503 `EXTERNAL_SERVICE_UNAVAILABLE`.
- Tests con `MockRestServiceServer` y los proxies reales (`NominatimGeocodificadorTest`).

El buscador de direcciones usa dos servicios de OpenStreetMap, ambos llamados desde el backend:

- **Sugerencias mientras se escribe** (`GET /direcciones/sugerencias?texto=&latitud=&longitud=&zoom=`): Photon, que está hecho para autocompletar.
  - El frontend pide después de 400 ms sin teclear y solo con 3 caracteres o más.
  - Envía el centro y el zoom del mapa redondeados a unos 1 km. Así las sugerencias cercanas a lo que se está mirando aparecen primero, sin excluir el resto de Chile. Sin esos tres parámetros, se sugiere en todo Chile por igual.
  - A Photon le llega un zoom de 12 como máximo. Con más zoom, su radio de cercanía es tan chico que, si no hay coincidencias muy cerca, vuelve al orden mundial, y cerca de la frontera ese orden lo dominan resultados de Argentina.
  - Se descartan los lugares de otros países (el recuadro también toma parte de Argentina) y los nombres repetidos (por ejemplo, los tramos de una misma calle), y se muestran hasta 5.
  - La caché `sugerencias` evita repetir. No reintenta: la siguiente tecla vuelve a pedir.
  - Variables: `SIANTE_AUTOCOMPLETADO_URL` (`https://photon.komoot.io`, o un Photon propio), `SIANTE_AUTOCOMPLETADO_TIEMPO_LIMITE` (3 s) y `SIANTE_AUTOCOMPLETADO_RECUADRO` (oeste,sur,este,norte; por defecto Chile con Isla de Pascua). Los países son los de `SIANTE_GEOCODIFICACION_PAISES`.
- **Búsqueda completa al presionar Enter** sin elegir una sugerencia (`GET /direcciones?texto=`): Nominatim, que exige identificar la aplicación, admite máximo una solicitud por segundo y **prohíbe autocompletar**; por eso no se usa para las sugerencias. Además, es el respaldo si Photon no responde. Variables: `SIANTE_GEOCODIFICACION_URL` (para apuntar a un Nominatim propio), `SIANTE_GEOCODIFICACION_TIEMPO_LIMITE` (5 s) y `SIANTE_GEOCODIFICACION_PAISES` (`cl`; vacío busca en todo el mundo).

En k8s, el pod necesita salida a internet hacia los dos servidores. Lo que escriben los usuarios llega a ambos servicios externos; si eso deja de ser aceptable, las dos URLs se pueden apuntar a instancias propias.

## Stack y versiones

- Java: JDK 25 LTS. Maven 3.9.16 mediante el wrapper incluido (`mvnw`). En la imagen se ejecuta con SapMachine 25.0.5 (JRE).
- Spring Boot: 4.1, con Tomcat embebido. Driver Oracle `ojdbc11` en la versión que fija Spring Boot.
- Node.js: 26.12.1 LTS (`frontend/.nvmrc`), solo para compilar; en producción el frontend lo sirve nginx 1.30.
- Angular: 22.

Las imágenes base de los Dockerfile van con versión exacta: actualizar una es un cambio visible en git. Ante un boletín de seguridad (CPU de Oracle, Node.js), subir esas versiones y reconstruir.

## Inicio rápido

### Backend

Definir las variables de conexión (`SIANTE_DATASOURCE_URL`, `SIANTE_DATASOURCE_USERNAME`, `SIANTE_DATASOURCE_PASSWORD`) en el `.env` de la raíz, que el backend lee solo; están descritas en `backend/src/main/resources/application.properties`. No guardar credenciales en el repositorio. Sin base de datos el backend no arranca; los tests sí corren sin ella.

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

La aplicación queda en `http://localhost:4200/`: la configuración `development` de `angular.json` usa `baseHref: "/"`, y la imagen de nginx que se despliega sigue en `/siante2/`. El proxy de desarrollo (`src/proxy.conf.json`) redirige `/siante2/api` al backend en `http://localhost:8080`.

### Con Docker

Crear `.env` en la raíz (ignorado por git) con `SIANTE_DATASOURCE_URL`, `SIANTE_DATASOURCE_USERNAME` y `SIANTE_DATASOURCE_PASSWORD`, y ejecutar `docker compose up --build` desde la raíz. Las demás variables y sus valores por defecto están en `backend/src/main/resources/application.properties`.

## Tests

```bash
cd backend
./mvnw test
```

- `ConfiguracionWebTest` y `CorsConfigTest` prueban prefijo, caché, errores y CORS sobre `ControladorPrueba`, un controlador que solo existe en `src/test`.
- Los tests corren con el perfil `test` (lo activa surefire): usan toda la configuración de `src/main/resources` y solo cambian la base por H2 (`src/test/resources/application-test.properties`). `ContextoAplicacionTest` levanta el contexto completo y `ActuatorTest` verifica salud, info y métricas.
- Acceso a datos solo con JPA (Spring Data). IDs: las entidades de tablas con trigger usan `@GeneratedValue(strategy = IDENTITY)` y Hibernate recupera el ID que pone la base (en vez del "máximo ID" de SIATE); los correlativos de reportes públicos se leen por JPQL de las vistas `NEXTVAL_*`. Se prueba en Oracle Free con trigger y secuencia.
- Geometrías: `GmlAGeometria` (lectura, GML de `SDO_UTIL.TO_GMLGEOMETRY` → GeoJSON) y `GeometriaAWkt` (escritura validada, con orientación de anillos corregida, por `SDO_UTIL.FROM_WKTGEOMETRY`) tienen tests unitarios. `GeometriaOracleTest` guarda cada tipo en Oracle Free y verifica que Oracle lo valide; usa Docker y no corre en el build normal: `./mvnw test -Poracle` (imagen `gvenzl/oracle-free:23-faststart`, unos 8 GB). Oracle Free no trae Java en la base, así que `TO_GMLGEOMETRY` solo se puede probar contra una base de la CGR.
- Prueba de concepto con PostgreSQL: `./mvnw test -Ppostgres` levanta PostgreSQL 16 en Docker (sin PostGIS: la geometría va en `JSON_METADATA`), Flyway carga el esquema espejo de Oracle (`src/test/resources/db/postgresql`) y prueba los adaptadores de hoy sobre PostgreSQL. Solo en los tests: la aplicación sigue en Oracle. Resultados y brechas en `docs/modelo-datos.md`.
- `ConsultasJpqlTest` compila cada `@Query` JPQL de los repositorios de todos los módulos contra el modelo de entidades, sin conectarse a Oracle.
- `ContratoApiTest` compara el contrato OpenAPI que genera la API (`/siante2/api/openapi`) con el versionado en `src/test/resources/contrato/openapi.json`. Si un cambio de la API es intencional, se regenera con `./mvnw test -Dtest=ContratoApiTest "-Dcontrato.actualizar=true"` y el diff del contrato se revisa en el PR. En producción el endpoint está desactivado (perfil `prod`).

```bash
cd frontend
npm test
npm run lint
```

Cobertura: `./mvnw verify` genera `target/site/jacoco/index.html` y exige 90 % de líneas y 65 % de ramas (sin contar entidades JPA ni la clase de arranque); el Dockerfile corre `verify`.

Los dos Dockerfile corren los tests (y el del frontend, también el lint) antes del build: si alguno falla, la imagen no se construye.

## Despliegue

Flujo GitOps:

1. Se hace push a `develop`.
2. Un pipeline externo construye las imágenes con `backend/Dockerfile` y `frontend/Dockerfile` y las publica en `ghcr.io`.
3. Argo CD sincroniza los manifiestos de `k8s/`.

| Ambiente | Manifiestos | Namespace | Réplicas |
|---|---|---|---|
| Dev/QA | `k8s/dev` | `sisgeob-test` | 1 de cada aplicación |
| Producción | `k8s/prod` | `sisgeob-prod` | 2, con rolling update sin indisponibilidad |

Los namespaces son compartidos con otras aplicaciones del área; las imágenes son `ghcr.io/cgr-chile/siante2-{backend,frontend}:dev` y `:prod` con `imagePullPolicy: Always`, y sin ConfigMap. Como el namespace es compartido, todos los recursos llevan el prefijo `siante2-` para no chocar con los de otras aplicaciones.

El ingress envía `/siante2/api` al backend y el resto al frontend.

El secreto real (`k8s/*/secret.yml`) lo define Ingeniería y está ignorado por git. `secret.example.yml` lista las credenciales que espera el backend (`SIANTE_DATASOURCE_*`). `kustomization.yml` referencia `secret.yml`, así que debe existir antes de aplicar los manifiestos.

Cada deployment del backend fija `SPRING_PROFILES_ACTIVE` (`dev` o `prod`) y `TZ=America/Santiago`. El resto (CORS, `JAVA_OPTS`) toma su valor por defecto; con `JAVA_OPTS` vacío, el heap queda en el 75 % de `resources.limits.memory` (ver `backend/Dockerfile`).

### Identidad

WebSEAL autentica al usuario y entrega al backend las cabeceras `iv-user` e `iv-groups`, igual que en SIATE. WebSEAL y la red los define Ingeniería. La aplicación parte de un supuesto: solo WebSEAL puede llegar al ingress, y por eso confía en esas cabeceras. La seguridad propia se monta sobre esa identidad (ver "Autorización").

`IdentidadWebSealFilter` convierte las cabeceras en el usuario de Spring Security (grupos como `GRUPO_<nombre>`) y lo agrega a los logs como `usuario`. Las reglas son las de `Auth` de SIATE:
- las dos cabeceras deben llegar;
- `Unauthenticated` en cualquiera de ellas no autentica;
- `iv-groups` vacío es un usuario sin grupos.

Además, no autentica un `iv-user` de más de 20 caracteres, el largo de `USERS.IV_USER`, porque no se podría registrar. Una cabecera con caracteres de control, como un salto de línea que inyectaría líneas falsas en los logs, ya la rechaza con 400 el firewall de Spring Security antes de llegar al filtro. Toda la API exige usuario, salvo el contrato OpenAPI. En el puerto de Actuator, la salud y la info no piden usuario y las métricas sí; sin usuario responde 401 `{code, message}`.

`GET /siante2/api/usuarios/actual` devuelve el usuario y lo registra en `USERS` la primera vez, como `/auth` de SIATE. El frontend lo consulta al entrar (`SesionService`, `sesionGuard`) y, si no hay permiso, muestra `/no-autorizado`.

"Salir", en el menú, hace lo mismo que en SIATE: cierra la ventana. El navegador lo impide si la pestaña no la abrió un script o si ya tiene historial; en ese caso queda a la vista `/salida`, con un enlace para volver a ingresar. No cierra la sesión de WebSEAL, que pertenece a otro sistema.

En el equipo no hay WebSEAL: con el perfil `local`, `SIANTE_USUARIO_LOCAL` en el `.env` define el `iv-user` que se asume. En `dev` y `prod` esa variable se ignora.

Las peticiones que modifican datos llevan protección CSRF por cookie (`XSRF-TOKEN`), que Angular envía solo.

### Autorización

En SIATE, WebSEAL era el único control. Los endpoints tomaban el usuario de la URL (`/users/:ID/layers`) y no comprobaban de quién era cada recurso. Cada módulo migrado aplica estas reglas:

- **El usuario sale de la identidad, nunca del cliente.** El ID de `USERS` se obtiene de `iv-user`; ninguna ruta ni cuerpo lo recibe. Las rutas hablan de recursos propios (`/capas/mias`, `/favoritos`), no de `/usuarios/{id}/...`.
- **Dueño en cada escritura.** Editar o borrar una capa, un grupo, un favorito o un punto del catálogo exige que sea del usuario. Si no lo es, responde 404 `NOT_FOUND`, para no revelar que el recurso existe.
- **Visibilidad en la consulta.** Las capas privadas solo las ve su dueño, y el filtro va en el `where`, no en memoria.
- **Grupos como roles solo si el negocio lo define.** SIATE guardaba `iv-groups` pero no lo usaba para permitir nada. Una función que requiera un grupo se protege con `@PreAuthorize("hasAuthority('GRUPO_<nombre>')")`. Va `hasAuthority` y no `hasRole`, porque `hasRole` antepone `ROLE_` y negaría siempre. Sin el grupo responde 403 `FORBIDDEN`.
- **Endpoints públicos explícitos.** Solo la lectura de los reportes con QR queda abierta, ruta por ruta, en `SeguridadConfig`. Todo lo demás exige usuario.
- **Auditoría.** Toda escritura deja un log `INFO` con la acción y el ID del recurso; el usuario y la solicitud ya van en cada línea.
- **Servicios de terceros solo desde el backend y con destino permitido.** Un proxy (por ejemplo `featureinfo` o `GetCapabilities` de WMS) solo consulta servidores de capas registradas, nunca una URL arbitraria que mande el cliente.

### Perfiles y logs

| Perfil | Dónde | Qué cambia |
|---|---|---|
| `local` (por omisión) | `mvnw spring-boot:run`, docker compose | Lee el `.env` de la raíz; logs en texto |
| `dev` | `k8s/dev` | Logs en JSON (ECS), contrato OpenAPI publicado |
| `prod` | `k8s/prod` | Logs en JSON (ECS), sin contrato OpenAPI |

Cada solicitud lleva un identificador (`X-Request-Id`, se reutiliza el del ingress si viene) que aparece en todas sus líneas de log como `solicitud`. El nivel de log del código propio se ajusta con `LOG_LEVEL_SIANTE2` (por omisión `INFO`).
