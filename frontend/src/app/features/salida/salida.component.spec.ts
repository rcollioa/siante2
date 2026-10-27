import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SalidaComponent } from './salida.component';

describe('SalidaComponent', () => {
  it('invita a cerrar la pestaña y ofrece volver a ingresar al mapa', () => {
    TestBed.configureTestingModule({ imports: [SalidaComponent], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(SalidaComponent);
    fixture.detectChanges();
    const elemento: HTMLElement = fixture.nativeElement;

    expect(elemento.querySelector('h1')?.textContent).toBe('Ha salido de SIATE');
    expect(elemento.querySelector('a.volver')?.getAttribute('href')).toBe('/mapa');
  });
});
