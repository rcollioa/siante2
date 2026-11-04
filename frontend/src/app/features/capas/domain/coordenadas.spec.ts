import { gmdAGrados, gmsAGrados, mgrsAGrados, numeroDe, utmAGrados } from './coordenadas';

describe('coordenadas', () => {
  it('lee números con punto o coma decimal', () => {
    expect(numeroDe(' -33,44 ')).toBe(-33.44);
    expect(numeroDe('-70.65')).toBe(-70.65);
    expect(numeroDe('')).toBeNaN();
    expect(numeroDe('sin dato')).toBeNaN();
  });

  describe('UTM', () => {
    it('convierte un punto del hemisferio norte: la Torre CN, 17T 630084 4833438', () => {
      const [longitud, latitud] = utmAGrados('17T', 630084, 4833438)!;

      expect(latitud).toBeCloseTo(43.6426, 3);
      expect(longitud).toBeCloseTo(-79.3871, 3);
    });

    it('convierte un punto del hemisferio sur: la Ópera de Sídney, 56H 334873 6252266', () => {
      const [longitud, latitud] = utmAGrados('56 h', 334873, 6252266)!;

      expect(latitud).toBeCloseTo(-33.8568, 3);
      expect(longitud).toBeCloseTo(151.2153, 3);
    });

    it.each([
      ['19', 300000, 6000000],
      ['61H', 300000, 6000000],
      ['19I', 300000, 6000000],
      ['19H', NaN, 6000000]
    ])('rechaza la zona %s con este %d', (zona, este, norte) => {
      expect(utmAGrados(zona, este, norte)).toBeNull();
    });
  });

  describe('grados, minutos y segundos', () => {
    it('sur y oeste son negativos, con S, O o W', () => {
      expect(gmsAGrados('33 30 0 S', '70 15 0 O')).toEqual([-70.25, -33.5]);
      expect(gmsAGrados("33° 30' 0'' S", "70° 15' 0'' W")).toEqual([-70.25, -33.5]);
    });

    it('norte y este son positivos, y también vale el signo', () => {
      expect(gmsAGrados('10 30 0 N', '20 15 0 E')).toEqual([20.25, 10.5]);
      expect(gmsAGrados('-33 30 0', '-70 15 0')).toEqual([-70.25, -33.5]);
    });

    it('sin todas sus partes no es una coordenada', () => {
      expect(gmsAGrados('33 30 S', '70 15 0 O')[1]).toBeNaN();
    });
  });

  it('grados y minutos decimales, con coma o punto', () => {
    expect(gmdAGrados('33 30,5 S', '70 15.5 O')).toEqual([-(70 + 15.5 / 60), -(33 + 30.5 / 60)]);
    expect(gmdAGrados('33 30 15 S', '70 15 O')[1]).toBeNaN();
  });

  describe('cuadrícula militar', () => {
    it('convierte el punto, sin importar espacios ni mayúsculas', () => {
      const [longitud, latitud] = mgrsAGrados('19h cc 46625 98715')!;

      expect(longitud).toBeCloseTo(-70.65, 3);
      expect(latitud).toBeCloseTo(-33.44, 3);
    });

    it('un texto inválido no es una coordenada', () => {
      expect(mgrsAGrados('no es mgrs')).toBeNull();
    });
  });
});
