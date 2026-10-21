import { API_PREFIJO, esRutaApi } from './api-prefijo';

describe('esRutaApi', () => {
  it('reconoce las rutas del backend propio', () => {
    expect(esRutaApi(`${API_PREFIJO}/ejemplo/anios`)).toBe(true);
  });

  it('no confunde el prefijo genérico /api ni los archivos del frontend', () => {
    expect(esRutaApi('/api/ejemplo/anios')).toBe(false);
    expect(esRutaApi('assets/img/logo.svg')).toBe(false);
    expect(esRutaApi(API_PREFIJO)).toBe(false);
  });
});
