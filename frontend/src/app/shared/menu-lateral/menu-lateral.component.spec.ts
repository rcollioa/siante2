import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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

  function crear(): ComponentFixture<MenuLateralComponent> {
    TestBed.configureTestingModule({
      imports: [MenuLateralComponent],
      providers: [provideRouter([{ path: '**', children: [] }]), provideHttpClient(), provideHttpClientTesting()]
    });
    TestBed.inject(SesionService).usuario.set(usuario);
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
    expect(pendientes.length).toBe(6);
    expect(pendientes[0].textContent).toContain('Mis capas');
  });

  it('muestra el logo, el usuario y su correo en el menú, como SIANTE', () => {
    const elemento: HTMLElement = crear().nativeElement;

    expect(elemento.querySelector('.marca img')?.getAttribute('src')).toBe('assets/img/logo-siante.svg');
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

  it('Salir finaliza la sesión, lleva a la página de salida e intenta cerrar la ventana, como SIANTE', async () => {
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
