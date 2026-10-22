import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MenuLateralComponent } from './menu-lateral.component';

describe('MenuLateralComponent', () => {
  function crear(): ComponentFixture<MenuLateralComponent> {
    TestBed.configureTestingModule({ imports: [MenuLateralComponent], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(MenuLateralComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('enlaza las secciones migradas y marca las demás como próximamente', () => {
    const elemento: HTMLElement = crear().nativeElement;

    const enlaces = Array.from(elemento.querySelectorAll('a')).map((a) => a.textContent?.trim());
    const pendientes = elemento.querySelectorAll('[aria-disabled="true"]');

    expect(enlaces).toEqual(['Inicio']);
    expect(pendientes.length).toBe(6);
    expect(pendientes[0].textContent).toContain('Mis capas');
  });

  it('el botón Menú abre y cierra la lista', () => {
    const fixture = crear();
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('button.alternar');

    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');

    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('false');
  });
});
