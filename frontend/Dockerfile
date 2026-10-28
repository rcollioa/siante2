# Multi-stage build: compila Angular y sirve los estáticos con Nginx
FROM node:26.12.1-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
# npm ci: instala exactamente lo del package-lock.json, igual en cada build.
RUN npm ci --no-audit --no-fund
COPY . .
# Lint (ESLint) y tests unitarios (Vitest, sin navegador): si alguno falla, la imagen no se construye, igual que el
# backend con Maven. NG_CLI_ANALYTICS evita la pregunta de telemetría del CLI de Angular.
ENV NG_CLI_ANALYTICS=false
RUN npm run lint
RUN npm test
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.30.6-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx-cabeceras-seguridad.conf /etc/nginx/snippets/cabeceras-seguridad.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html/siante2
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
