import { LOCALE_ID, signal } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapaEnMapa, CapasAbiertas, ElementoElegido, PuntoConsultado } from '../../../application/capas-abiertas';
import { EdicionElementos } from '../../../application/edicion-elementos';
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
    editable: true,
    tipo: 'VECTORIAL',
    elementos: Array.from({ length: 1234 }, () => elemento),
    servicio: null,
    ...cambios
  });
  const wms = (id: number, nombre: string): Capa =>
    capa({ id, nombre, tipo: 'URL', elementos: [], servicio: { tipo: 'WMS', capas: [], urlMapa: '/siante2/api/capas/7/wms', disponible: true, grilla: null } });
  const servicio = {
    abiertas: signal<CapaEnMapa[]>([]),
    abriendo: signal<ReadonlySet<number>>(new Set()),
    error: signal(''),
    elegido: signal<ElementoElegido | null>(null),
    consultado: signal<PuntoConsultado | null>(null),
    sinRespuesta: signal<Capa[]>([]),
    deseleccionar: vi.fn(),
    descartarError: vi.fn()
  };
  const edicion = { capa: signal<Capa | null>(null), seleccionado: signal<ElementoCapa | null>(null), cambiarDatos: vi.fn() };
  let fixture: ComponentFixture<PanelCapaComponent>;
  const texto = () => (fixture.nativeElement as HTMLElement).textContent?.replace(/\s+/g, ' ').trim() ?? '';

  beforeEach(() => {
    registerLocaleData(localeEsCl, 'es-CL');
    vi.clearAllMocks();
    servicio.abiertas.set([]);
    servicio.abriendo.set(new Set());
    servicio.error.set('');
    servicio.elegido.set(null);
    servicio.consultado.set(null);
    servicio.sinRespuesta.set([]);
    TestBed.configureTestingModule({
      imports: [PanelCapaComponent],
      providers: [
        { provide: CapasAbiertas, useValue: servicio },
        { provide: EdicionElementos, useValue: edicion },
        { provide: LOCALE_ID, useValue: 'es-CL' }
      ]
    });
    fixture = TestBed.createComponent(PanelCapaComponent);
  });

  describe('mientras se dibuja', () => {
    afterEach(() => {
      edicion.capa.set(null);
      edicion.seleccionado.set(null);
    });

    it('un elemento de la capa en edición se edita, en vez de solo mostrar su información', () => {
      edicion.capa.set(capa());
      edicion.seleccionado.set(elemento);
      servicio.elegido.set({ capa: capa(), elemento });
      fixture.detectChanges();

      const nombre = (fixture.nativeElement as HTMLElement).querySelector('#nombre-elemento') as HTMLInputElement;
      expect(nombre.value).toBe('Bodega central');
    });

    it('un elemento de otra capa abierta solo se puede mirar', () => {
      edicion.capa.set(capa({ id: 2 }));
      servicio.elegido.set({ capa: capa(), elemento });
      fixture.detectChanges();

      expect((fixture.nativeElement as HTMLElement).querySelector('#nombre-elemento')).toBeNull();
      expect(texto()).toContain('Bodega central');
      servicio.elegido.set(null);
    });

    it('no repite cómo elegir un elemento: lo indica la barra de dibujo', () => {
      edicion.capa.set(capa());
      servicio.abiertas.set([{ capa: capa(), visible: true }]);
      fixture.detectChanges();

      expect(texto()).toBe('');
    });
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

  describe('información de un punto en capas WMS', () => {
    beforeEach(() => servicio.abiertas.set([{ capa: wms(7, 'IDE'), visible: true }]));

    it('mientras consulta lo dice', () => {
      servicio.consultado.set({ consultando: true, resultados: [], sinRespuesta: [] });
      fixture.detectChanges();

      expect(texto()).toBe('Información del punto×Consultando a los servidores de mapas…');
    });

    it('muestra los datos de cada elemento con su capa y la del servidor, y permite cerrarla', () => {
      servicio.consultado.set({
        consultando: false,
        resultados: [{ capa: wms(7, 'IDE'), elementos: [{ capa: 'Ríos', datos: [{ nombre: 'NOMBRE', valor: 'Maipo' }] }, { capa: '', datos: [] }] }],
        sinRespuesta: [wms(8, 'Geoportal')]
      });
      fixture.detectChanges();

      const panel: HTMLElement = fixture.nativeElement;
      const notas = [...panel.querySelectorAll('.resultado .nota')].map((nota) => nota.textContent?.trim());
      expect(notas).toEqual(['IDE · Ríos', 'IDE']);
      expect(panel.querySelector('dt')?.textContent).toBe('NOMBRE');
      expect(panel.querySelector('dd')?.textContent).toBe('Maipo');
      expect(texto()).toContain('El servidor no entregó datos de este elemento.');
      expect(texto()).toContain('El servidor de mapas de «Geoportal» no respondió la consulta.');

      panel.querySelector<HTMLButtonElement>('button.cerrar')!.click();
      expect(servicio.deseleccionar).toHaveBeenCalled();
    });

    it('si no hay nada en el punto lo dice', () => {
      servicio.consultado.set({ consultando: false, resultados: [], sinRespuesta: [] });
      fixture.detectChanges();

      expect(texto()).toBe('Información del punto×No hay información de las capas en este punto.');
    });
  });

  it('avisa que las capas WFS todavía no se pueden ver', () => {
    servicio.abiertas.set([{ capa: capa({ tipo: 'URL', elementos: [], servicio: { tipo: 'WFS', capas: [], urlMapa: '', disponible: true, grilla: null } }), visible: true }]);
    fixture.detectChanges();

    expect(texto()).toBe('Las capas por URL de tipo WFS se podrán ver en una próxima versión.');
  });

  it('con solo capas WMS que responden no ocupa el mapa', () => {
    servicio.abiertas.set([{ capa: wms(7, 'IDE'), visible: true }]);
    fixture.detectChanges();

    expect(texto()).toBe('');
  });

  it('avisa qué servidores de mapas no responden', () => {
    servicio.abiertas.set([{ capa: wms(7, 'IDE'), visible: true }]);
    servicio.sinRespuesta.set([wms(7, 'IDE')]);
    fixture.detectChanges();

    expect(texto()).toBe('El servidor de mapas de «IDE» no responde.');
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
