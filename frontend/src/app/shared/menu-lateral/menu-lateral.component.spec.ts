import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
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
      providers: [provideRouter([{ path: '**', children: [] }]), provideHttpClient(), provideHttpClientTesting()]
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

  it('enlaza las secciones migradas y marca las demás como próximamente', () => {
    const elemento: HTMLElement = crear().nativeElement;

    const enlaces = Array.from(elemento.querySelectorAll('ul a')).map((a) => a.textContent?.trim());
    const pendientes = elemento.querySelectorAll('[aria-disabled="true"]');

    expect(enlaces).toEqual(['Inicio', 'Ayuda']);
    expect(pendientes.length).toBe(5);
    expect(pendientes[0].textContent).toContain('Favoritos');
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
