import { LOCALE_ID, signal } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapaAbiertaService } from './capa-abierta.service';
import { Capa, ElementoCapa } from './capas-api.service';
import { PanelCapaComponent } from './panel-capa.component';

describe('PanelCapaComponent', () => {
  const elemento: ElementoCapa = {
    id: 0,
    geometria: { type: 'Point', coordinates: [-70.65, -33.44], geometries: [] },
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
    capa: signal<Capa | null>(null),
    abriendo: signal<number | null>(null),
    error: signal(''),
    elemento: signal<ElementoCapa | null>(null),
    cerrar: vi.fn()
  };
  let fixture: ComponentFixture<PanelCapaComponent>;
  const texto = () => (fixture.nativeElement as HTMLElement).textContent?.replace(/\s+/g, ' ').trim() ?? '';

  beforeEach(() => {
    registerLocaleData(localeEsCl, 'es-CL');
    servicio.capa.set(null);
    servicio.abriendo.set(null);
    servicio.error.set('');
    servicio.elemento.set(null);
    TestBed.configureTestingModule({
      imports: [PanelCapaComponent],
      providers: [{ provide: CapaAbiertaService, useValue: servicio }, { provide: LOCALE_ID, useValue: 'es-CL' }]
    });
    fixture = TestBed.createComponent(PanelCapaComponent);
  });

  it('sin capa abierta no muestra nada', () => {
    fixture.detectChanges();
    expect(texto()).toBe('');
  });

  it('muestra la capa abierta con su cantidad de elementos y permite cerrarla', () => {
    servicio.capa.set(capa());
    fixture.detectChanges();

    expect(texto()).toContain('Bodegas');
    expect(texto()).toContain('1.234 elementos. Haga clic en uno para ver su información.');
    fixture.nativeElement.querySelector('button.cerrar').click();
    expect(servicio.cerrar).toHaveBeenCalled();
  });

  it('muestra la información del elemento en que se hizo clic', () => {
    servicio.capa.set(capa());
    servicio.elemento.set(elemento);
    fixture.detectChanges();

    const panel: HTMLElement = fixture.nativeElement;
    expect(panel.querySelector('h3')?.textContent).toBe('Bodega central');
    expect(texto()).toContain('Junto al río');
    expect(panel.querySelector('dt')?.textContent).toBe('Dirección');
    expect(panel.querySelector('dd')?.textContent).toBe('Teatinos 56');
  });

  it('avisa que las capas por URL todavía no se pueden ver', () => {
    servicio.capa.set(capa({ tipo: 'URL', elementos: [] }));
    fixture.detectChanges();

    expect(texto()).toContain('Las capas por URL (WMS, WFS o WMTS) se podrán ver en una próxima versión.');
  });

  it('mientras abre y si falla lo dice', () => {
    servicio.abriendo.set(1);
    fixture.detectChanges();
    expect(texto()).toBe('Abriendo la capa…');

    servicio.abriendo.set(null);
    servicio.error.set('No se pudo abrir la capa. Intente nuevamente.');
    fixture.detectChanges();
    expect(texto()).toContain('No se pudo abrir la capa.');

    fixture.nativeElement.querySelector('button.boton').click();
    expect(servicio.error()).toBe('');
  });
});
