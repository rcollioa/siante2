// Node 25 o superior trae su propio localStorage y sessionStorage (Web Storage), que sin --localstorage-file
// quedan vacíos y tapan los de jsdom. Las pruebas usan los de jsdom, como el navegador.
declare const jsdom: { window: Window };

for (const almacenamiento of ['localStorage', 'sessionStorage'] as const) {
  Object.defineProperty(globalThis, almacenamiento, {
    value: jsdom.window[almacenamiento],
    configurable: true,
    writable: true
  });
}
