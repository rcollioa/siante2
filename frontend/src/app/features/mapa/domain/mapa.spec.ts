import { formatearDistancia } from './mapa';

describe('formatearDistancia', () => {
  it('muestra metros hasta 1 km, con un decimal y coma decimal', () => {
    expect(formatearDistancia(150)).toBe('150 m');
    expect(formatearDistancia(12.34)).toBe('12,3 m');
    expect(formatearDistancia(999.9)).toBe('999,9 m');
  });

  it('desde 1 km muestra kilómetros con dos decimales y punto de miles', () => {
    expect(formatearDistancia(1000)).toBe('1 km');
    expect(formatearDistancia(2345.6)).toBe('2,35 km');
    expect(formatearDistancia(1_234_567)).toBe('1.234,57 km');
  });
});
