# Multi-stage build: compila Angular y sirve los estáticos con Nginx
FROM node:22.23.4-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
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
