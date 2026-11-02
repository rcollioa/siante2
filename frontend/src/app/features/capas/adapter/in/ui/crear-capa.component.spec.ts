import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { MisCapas } from '../../../application/mis-capas';
import { CapaPropia, NuevaCapa } from '../../../domain/capa';
import { CrearCapaComponent } from './crear-capa.component';

describe('CrearCapaComponent', () => {
  const creada: CapaPropia = { id: 50, nombre: 'Bodegas', descripcion: 'Región', visibilidad: 'PRIVADA', categoriaId: 0 };
  const crear = vi.fn<(nueva: NuevaCapa) => Observable<CapaPropia>>();
  let fixture: ComponentFixture<CrearCapaComponent>;
  let elemento: HTMLElement;
  let recibidas: CapaPropia[];

  const dialogo = () => elemento.querySelector('dialog') as HTMLDialogElement;
  const nombre = () => elemento.querySelector('#nombre-nueva-capa') as HTMLInputElement;
  const descripcion = () => elemento.querySelector('#descripcion-nueva-capa') as HTMLTextAreaElement;
  const aplicar = () => elemento.querySelector('button[type="submit"]') as HTMLButtonElement;
  const errores = () => Array.from(elemento.querySelectorAll('.error')).map((e) => e.textContent?.trim());

  function escribir(campo: HTMLInputElement | HTMLTextAreaElement, valor: string): void {
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  /** Enviar el formulario como lo hace el botón, que jsdom no dispara con un clic. */
  function enviar(): void {
    (elemento.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  function abrir(): void {
    fixture.componentInstance.abrir();
    fixture.detectChanges();
  }

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
    crear.mockReset();
    crear.mockReturnValue(of(creada));
    recibidas = [];
    TestBed.configureTestingModule({ providers: [{ provide: MisCapas, useValue: { crear } }] });
    fixture = TestBed.createComponent(CrearCapaComponent);
    fixture.componentInstance.creada.subscribe((capa) => recibidas.push(capa));
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  });

  it('abre vacío y sin mostrar errores', () => {
    abrir();

    expect(dialogo().open).toBe(true);
    expect(nombre().value).toBe('');
    expect(errores()).toEqual([]);
    expect(document.activeElement).toBe(nombre());
  });

  it('crea la capa, cierra y avisa cuál se creó', () => {
    abrir();
    escribir(nombre(), 'Bodegas');
    escribir(descripcion(), 'Región');

    enviar();

    expect(crear).toHaveBeenCalledWith({ nombre: 'Bodegas', descripcion: 'Región' });
    expect(dialogo().open).toBe(false);
    expect(recibidas).toEqual([creada]);
  });

  it('con un nombre inválido muestra el error de SIATE y no crea', () => {
    abrir();

    enviar();
    expect(errores()).toEqual(['Nombre requerido.']);
    expect(nombre().getAttribute('aria-invalid')).toBe('true');
    expect(nombre().getAttribute('aria-describedby')).toBe('error-nombre-nueva-capa');

    escribir(nombre(), 'abc');
    expect(errores()).toEqual(['El nombre debe tener al menos 4 caracteres.']);

    enviar();
    expect(crear).not.toHaveBeenCalled();
    expect(dialogo().open).toBe(true);
  });

  it('al salir del nombre ya muestra si falta', () => {
    abrir();

    nombre().dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    expect(errores()).toEqual(['Nombre requerido.']);
  });

  it('mientras crea no deja enviar dos veces', () => {
    const respuesta = new Subject<CapaPropia>();
    crear.mockReturnValue(respuesta);
    abrir();
    escribir(nombre(), 'Bodegas');

    enviar();
    expect(aplicar().disabled).toBe(true);
    expect(aplicar().textContent).toContain('Creando');

    enviar();
    expect(crear).toHaveBeenCalledOnce();

    respuesta.next(creada);
    expect(dialogo().open).toBe(false);
  });

  it('si falla lo explica, queda abierto y conserva lo escrito', () => {
    crear.mockReturnValue(throwError(() => new Error('sin conexión')));
    abrir();
    escribir(nombre(), 'Bodegas');

    enviar();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toBe('No se pudo crear la capa. Intente nuevamente.');
    expect(dialogo().open).toBe(true);
    expect(nombre().value).toBe('Bodegas');
    expect(aplicar().disabled).toBe(false);
    expect(recibidas).toEqual([]);
  });

  it('Cancelar cierra sin crear y al volver a abrir parte de nuevo', () => {
    abrir();
    escribir(nombre(), 'ab');
    enviar();

    (elemento.querySelector('.botones button[type="button"]') as HTMLButtonElement).click();
    expect(dialogo().open).toBe(false);
    expect(crear).not.toHaveBeenCalled();

    abrir();
    expect(nombre().value).toBe('');
    expect(errores()).toEqual([]);
  });
});
