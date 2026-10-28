import { LOCALE_ID, signal } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapaEnMapa, CapasAbiertas, ElementoElegido } from '../../../application/capas-abiertas';
import { Capa, ElementoCapa } from '../../../domain/capa';
import { PanelCapaComponent } from './panel-capa.component';

describe('PanelCapaComponent', () => {
  const elemento: ElementoCapa = {
    id: 0,
    geometria: { type: 'Point', coordinates: [-70.65, -33.44] },
    radio: 0,
    nombre: 'Bodega central',
    descripcion: 'Junto al río',
    estilo: { color: '', grosor: 0, relleno: '', icono: '' },
    metadatos: [{ nombre: 'Dirección', valor: 'Teatinos 56' }]
  };
  const capa = (cambios: Partial<Capa> = {}): Capa => ({
    id: 1,
    nombre: 'Bodegas',
    descripcion: '',
    visibilidad: 'PRIVADA',
    propia: true,
    tipo: 'VECTORIAL',
    elementos: Array.from({ length: 1234 }, () => elemento),
    ...cambios
  });
  const servicio = {
    abiertas: signal<CapaEnMapa[]>([]),
    abriendo: signal<ReadonlySet<number>>(new Set()),
    error: signal(''),
    elegido: signal<ElementoElegido | null>(null),
    deseleccionar: vi.fn(),
    descartarError: vi.fn()
  };
  let fixture: ComponentFixture<PanelCapaComponent>;
  const texto = () => (fixture.nativeElement as HTMLElement).textContent?.replace(/\s+/g, ' ').trim() ?? '';

  beforeEach(() => {
    registerLocaleData(localeEsCl, 'es-CL');
    vi.clearAllMocks();
    servicio.abiertas.set([]);
    servicio.abriendo.set(new Set());
    servicio.error.set('');
    servicio.elegido.set(null);
    TestBed.configureTestingModule({
      imports: [PanelCapaComponent],
      providers: [{ provide: CapasAbiertas, useValue: servicio }, { provide: LOCALE_ID, useValue: 'es-CL' }]
    });
    fixture = TestBed.createComponent(PanelCapaComponent);
  });

  it('sin capas abiertas no muestra nada', () => {
    fixture.detectChanges();
    expect(texto()).toBe('');
  });

  it('cuenta solo los elementos de las capas visibles e invita a elegir uno', () => {
    servicio.abiertas.set([
      { capa: capa(), visible: true },
      { capa: capa({ id: 2, elementos: [elemento] }), visible: false }
    ]);
    fixture.detectChanges();

    expect(texto()).toBe('1.234 elementos visibles. Haga clic en uno para ver su información.');
  });

  it('con todas las capas ocultas lo dice', () => {
    servicio.abiertas.set([{ capa: capa(), visible: false }]);
    fixture.detectChanges();

    expect(texto()).toBe('Las capas abiertas no tienen elementos a la vista.');
  });

  it('muestra la información del elemento en que se hizo clic, con su capa, y permite cerrarla', () => {
    servicio.abiertas.set([{ capa: capa(), visible: true }]);
    servicio.elegido.set({ capa: capa(), elemento });
    fixture.detectChanges();

    const panel: HTMLElement = fixture.nativeElement;
    expect(panel.querySelector('.nota')?.textContent).toBe('Bodegas');
    expect(panel.querySelector('h2')?.textContent).toBe('Bodega central');
    expect(texto()).toContain('Junto al río');
    expect(panel.querySelector('dt')?.textContent).toBe('Dirección');
    expect(panel.querySelector('dd')?.textContent).toBe('Teatinos 56');

    panel.querySelector<HTMLButtonElement>('button.cerrar')!.click();
    expect(servicio.deseleccionar).toHaveBeenCalled();
  });

  it('avisa que las capas por URL todavía no se pueden ver', () => {
    servicio.abiertas.set([{ capa: capa({ tipo: 'URL', elementos: [] }), visible: true }]);
    fixture.detectChanges();

    expect(texto()).toBe('Las capas por URL (WMS, WFS o WMTS) se podrán ver en una próxima versión.');
  });

  it('mientras abre y si falla lo dice', () => {
    servicio.abriendo.set(new Set([1, 2]));
    fixture.detectChanges();
    expect(texto()).toBe('Abriendo las capas…');

    servicio.abriendo.set(new Set());
    servicio.error.set('No se pudo abrir la capa. Intente nuevamente.');
    fixture.detectChanges();
    expect(texto()).toContain('No se pudo abrir la capa.');

    fixture.nativeElement.querySelector('button.boton').click();
    expect(servicio.descartarError).toHaveBeenCalled();
  });
});
