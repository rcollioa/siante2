// @ts-check
const eslint = require("@eslint/js");
const { defineConfig } = require("eslint/config");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

// Arquitectura hexagonal (README "Frontend"): qué puede importar cada capa de features/<modulo>/.
const ADAPTADORES_DE_SALIDA = {
  // Las rutas relativas de adapter/in al adapter/out del mismo módulo no pasan por "adapter".
  group: ["**/adapter/out/**", "../out/**", "../../out/**"],
  message: "Use el puerto: el adaptador solo se registra en <modulo>.providers.ts.",
};
const CONTRATO = {
  group: ["**/shared/api/contrato"],
  message: "Los tipos del contrato son del adaptador HTTP; use los del dominio.",
};
const OPENLAYERS = {
  group: ["ol", "ol/**"],
  message: "OpenLayers solo va en mapa/adapter/out/openlayers; use MapaPort.",
};
const HTTP = {
  group: ["@angular/common/http"],
  message: "HTTP solo va en un adaptador de salida; use un puerto.",
};
const ADAPTADORES = {
  group: ["**/adapter/**"],
  message: "El dominio y la aplicación no conocen los adaptadores.",
};
const restringir = (...patterns) => ({ "no-restricted-imports": ["error", { patterns }] });

module.exports = defineConfig([
  // Generado desde el contrato OpenAPI con `npm run api:tipos`.
  { ignores: ["src/app/shared/api/contrato.ts"] },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "app",
          style: "camelCase",
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: "app",
          style: "kebab-case",
        },
      ],
    },
  },
  {
    files: ["src/app/**/*.ts"],
    ignores: ["src/app/**/adapter/out/**", "src/app/**/*.providers.ts"],
    rules: restringir(ADAPTADORES_DE_SALIDA),
  },
  {
    files: ["src/app/features/*/domain/**/*.ts"],
    rules: restringir(ADAPTADORES, OPENLAYERS, HTTP, CONTRATO, {
      group: ["@angular/**", "rxjs", "rxjs/**", "**/application/**"],
      message: "El dominio es TypeScript puro: sin Angular, RxJS ni la capa de aplicación.",
    }),
  },
  {
    files: ["src/app/features/*/application/**/*.ts"],
    rules: restringir(ADAPTADORES, OPENLAYERS, HTTP, CONTRATO),
  },
  {
    files: ["src/app/features/*/adapter/in/**/*.ts"],
    rules: restringir(ADAPTADORES_DE_SALIDA, OPENLAYERS, HTTP, CONTRATO),
  },
  {
    files: ["**/*.html"],
    extends: [
      angular.configs.templateRecommended,
      angular.configs.templateAccessibility,
    ],
    rules: {},
  }
]);
