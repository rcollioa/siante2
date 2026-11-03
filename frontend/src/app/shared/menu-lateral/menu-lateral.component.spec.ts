import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { proveerCapas } from '../../features/capas/capas.providers';
import { proveerMapa } from '../../features/mapa/mapa.providers';
import { ArchivosSoltados } from '../archivos-soltados/archivos-soltados.service';
import { SesionService } from '../sesion/sesion.service';
import { MenuLateralComponent } from './menu-lateral.component';

describe('MenuLateralComponent', () => {
  const usuario = { id: 7, ivUser: 'jperez', nombre: 'Juan', apellido: 'Pérez', email: 'jperez@contraloria.cl', grupos: [] };

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function crear(conUsuario = true): ComponentFixture<MenuLateralComponent> {
    TestBed.configureTestingModule({
      imports: [MenuLateralComponent],
      providers: [provideRouter([{ path: '**', children: [] }]), provideHttpClient(), provideHttpClientTesting(), ...proveerMapa(), ...proveerCapas()]
    });
    const sesion = TestBed.inject(SesionService);
    if (conUsuario) {
      sesion.usuario.set(usuario);
      sesion.estado.set('activa');
    } else {
      sesion.estado.set('pendiente');
    }
    const fixture = TestBed.createComponent(MenuLateralComponent);
    fixture.detectChanges();
    return fixture;
  }

  function pantallaChica(): void {
    vi.stubGlobal('matchMedia', (consulta: string) => ({ matches: consulta === '(max-width: 48rem)' }));
  }

  it('muestra al pie la versión del backend, y nada si no llega', () => {
    const fixture = crear();
    const http = TestBed.inject(HttpTestingController);

    http.expectOne('/siante2/api/version').flush({ version: '1.2.0', compilada: '2026-10-29T17:28:03Z' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.version')?.textContent).toMatch(
      /SIATE · versión 1\.2\.0\s+del \d{2}\/10\/2026 \d{2}:\d{2}/
    );
  });

  it('sin versión no muestra el pie', () => {
    const fixture = crear();

    TestBed.inject(HttpTestingController).expectOne('/siante2/api/version').flush(null, { status: 503, statusText: 'No' });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.version')).toBeNull();
  });

  it('enlaza las secciones migradas y marca las demás como próximamente', () => {
    const elemento: HTMLElement = crear().nativeElement;

    const enlaces = Array.from(elemento.querySelectorAll('ul a')).map((a) => a.textContent?.trim());
    const desplegables = Array.from(elemento.querySelectorAll('button.desplegable')).map((b) => b.textContent?.replace('▾', '').trim());
    const pendientes = elemento.querySelectorAll('[aria-disabled="true"]');

    expect(enlaces).toEqual(['Inicio', 'Ayuda']);
    expect(desplegables).toEqual(['Mis capas', 'Favoritos', 'Capas formales']);
    expect(pendientes.length).toBe(3);
    expect(pendientes[0].textContent).toContain('Grupos de capas');
  });

  it('"Mis capas" se despliega con su lista, que se descarga recién al abrirla', async () => {
    // MisCapas abre las capas en el mapa, y OpenLayers usa ResizeObserver, que jsdom no trae.
    vi.stubGlobal('ResizeObserver', class { observe = vi.fn(); unobserve = vi.fn(); disconnect = vi.fn(); });
    const fixture = crear();
    const elemento: HTMLElement = fixture.nativeElement;
    const boton = elemento.querySelector<HTMLButtonElement>('button.desplegable')!;
    expect(boton.textContent).toContain('Mis capas');
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    expect(elemento.querySelector('app-mis-capas')).toBeNull();

    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');
    await vi.waitFor(() => {
      fixture.detectChanges();
      expect(elemento.querySelector('app-mis-capas')).not.toBeNull();
    });
    TestBed.inject(HttpTestingController)
      .expectOne('/siante2/api/capas/mias')
      .flush({ data: [{ id: 1, nombre: 'Bodegas', descripcion: '', visibilidad: 'PRIVADA' }] });
    fixture.detectChanges();
    expect(elemento.querySelector('app-mis-capas')?.textContent).toContain('Bodegas');

    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    expect(elemento.querySelector('app-mis-capas')).toBeNull();
  });

  describe('con un archivo soltado en la ventana', () => {
    const geojson = '{"type":"Point","coordinates":[-70.65,-33.44]}';
    const archivo = { name: 'Bodegas.geojson', size: geojson.length, arrayBuffer: async () => new TextEncoder().encode(geojson).buffer } as unknown as File;

    // jsdom no siempre implementa el diálogo modal: basta con marcarlo abierto o cerrado.
    beforeAll(() => {
      if (!HTMLDialogElement.prototype.showModal) {
        HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
          this.open = true;
        };
        HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
          this.open = false;
        };
      }
    });

    beforeEach(() => {
      vi.stubGlobal('ResizeObserver', class { observe = vi.fn(); unobserve = vi.fn(); disconnect = vi.fn(); });
    });

    it('abre el menú y despliega "Mis capas", que descarga su panel y abre el formulario con el archivo', async () => {
      pantallaChica();
      const fixture = crear();
      const elemento: HTMLElement = fixture.nativeElement;
      expect(elemento.querySelector('.menu')?.classList).not.toContain('abierto');

      TestBed.inject(ArchivosSoltados).pendientes.set([archivo]);
      fixture.detectChanges();

      expect(elemento.querySelector('.menu')?.classList).toContain('abierto');
      expect(elemento.querySelector('button.desplegable')?.getAttribute('aria-expanded')).toBe('true');
      await vi.waitFor(() => {
        fixture.detectChanges();
        expect(elemento.querySelector<HTMLDialogElement>('app-crear-capa dialog')?.open).toBe(true);
      });
      await vi.waitFor(() => {
        fixture.detectChanges();
        expect(elemento.querySelector('app-crear-capa [role="status"]')?.textContent).toContain('Se importará 1 elemento.');
      });
      expect(elemento.querySelector<HTMLInputElement>('#nombre-nueva-capa')?.value).toBe('Bodegas');
    });

    it('si "Mis capas" ya estaba desplegada no la pliega', async () => {
      const fixture = crear();
      const elemento: HTMLElement = fixture.nativeElement;
      const boton = elemento.querySelector<HTMLButtonElement>('button.desplegable')!;
      boton.click();
      await vi.waitFor(() => {
        fixture.detectChanges();
        expect(elemento.querySelector('app-mis-capas')).not.toBeNull();
      });

      TestBed.inject(ArchivosSoltados).pendientes.set([archivo]);
      fixture.detectChanges();

      expect(boton.getAttribute('aria-expanded')).toBe('true');
      expect(elemento.querySelector<HTMLDialogElement>('app-crear-capa dialog')?.open).toBe(true);
    });
  });

  it('mientras se consulta la sesión reserva la franja del usuario, sin Salir ni datos', () => {
    const elemento: HTMLElement = crear(false).nativeElement;

    const franja = elemento.querySelector('.usuario');
    expect(franja?.getAttribute('aria-hidden')).toBe('true');
    expect(franja?.textContent?.trim()).toBe('');
    expect(elemento.querySelector('button.salir')).toBeNull();
    expect(elemento.querySelectorAll('ul li')).toHaveLength(8);
  });

  it('muestra el logo, el usuario y su correo en el menú, como SIATE', () => {
    const elemento: HTMLElement = crear().nativeElement;

    expect(elemento.querySelector('.marca img')?.getAttribute('src')).toBe('assets/img/logo-siate.svg');
    expect(elemento.querySelector('.usuario')?.textContent).toContain('Usuario: Juan Pérez');
    expect(elemento.querySelector('.fila-usuario')?.textContent).toContain('Salir');
    expect(elemento.querySelector('.usuario a')?.getAttribute('href')).toBe('mailto:jperez@contraloria.cl');
  });

  it('empieza abierto en escritorio y el botón lo cierra y lo vuelve a abrir', () => {
    const fixture = crear();
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('button.alternar');
    expect(boton.getAttribute('aria-expanded')).toBe('true');

    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    expect(boton.getAttribute('aria-label')).toBe('Mostrar menú');

    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');
  });

  it('en pantallas chicas empieza cerrado y se cierra al elegir una sección', () => {
    pantallaChica();
    const fixture = crear();
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('button.alternar');
    expect(boton.getAttribute('aria-expanded')).toBe('false');

    boton.click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('ul a').click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('false');
  });

  it('Salir finaliza la sesión, lleva a la página de salida e intenta cerrar la ventana, como SIATE', async () => {
    const cerrar = vi.spyOn(window, 'close').mockImplementation(() => undefined);
    const fixture = crear();

    fixture.nativeElement.querySelector('.usuario button.salir').click();
    await fixture.whenStable();

    expect(TestBed.inject(SesionService).estado()).toBe('finalizada');
    expect(TestBed.inject(Router).url).toBe('/salida');
    expect(cerrar).toHaveBeenCalled();
  });

  it('en escritorio sigue abierto al elegir una sección', () => {
    const fixture = crear();

    fixture.nativeElement.querySelector('ul a').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button.alternar').getAttribute('aria-expanded')).toBe('true');
  });
});
