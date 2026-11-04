import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ConfirmacionComponent, Pregunta } from './confirmacion.component';

describe('ConfirmacionComponent', () => {
  let fixture: ComponentFixture<ConfirmacionComponent>;
  let elemento: HTMLElement;
  const accion = vi.fn<Pregunta['accion']>();
  const pregunta: Pregunta = {
    mensaje: '¿Confirma el borrado de la capa?',
    detalle: 'Bodegas',
    accion,
    mensajeDeError: () => 'No se pudo guardar.'
  };

  const dialogo = () => elemento.querySelector('dialog') as HTMLDialogElement;
  const si = () => elemento.querySelector('button.si') as HTMLButtonElement;
  const no = () => elemento.querySelector('button.no') as HTMLButtonElement;

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
    accion.mockReset();
    accion.mockReturnValue(of(null));
    fixture = TestBed.createComponent(ConfirmacionComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
    fixture.componentInstance.preguntar(pregunta);
    fixture.detectChanges();
  });

  it('muestra la pregunta y sobre qué se pregunta', () => {
    expect(dialogo().open).toBe(true);
    expect(elemento.textContent).toContain('¿Confirma el borrado de la capa?');
    expect(elemento.querySelector('.detalle')?.textContent).toBe('Bodegas');
  });

  it('"No" va primero, para que reciba el foco, y cierra sin hacer nada', () => {
    expect(elemento.querySelector('button')).toBe(no());

    no().click();

    expect(dialogo().open).toBe(false);
    expect(accion).not.toHaveBeenCalled();
  });

  it('"Sí" hace la acción y cierra', () => {
    si().click();

    expect(accion).toHaveBeenCalledOnce();
    expect(dialogo().open).toBe(false);
  });

  it('mientras se guarda no deja responder dos veces', () => {
    const respuesta = new Subject<null>();
    accion.mockReturnValue(respuesta);

    si().click();
    fixture.detectChanges();
    expect(si().disabled).toBe(true);
    expect(si().textContent).toContain('Guardando');
    expect(dialogo().open).toBe(true);

    respuesta.next(null);
    expect(dialogo().open).toBe(false);
  });

  it('si falla queda abierto con el error y se puede reintentar', () => {
    accion.mockReturnValue(throwError(() => new Error('sin conexión')));

    si().click();
    fixture.detectChanges();

    expect(dialogo().open).toBe(true);
    expect(elemento.querySelector('[role="alert"]')?.textContent).toBe('No se pudo guardar.');
    expect(si().disabled).toBe(false);

    accion.mockReturnValue(of(null));
    si().click();
    fixture.detectChanges();
    expect(dialogo().open).toBe(false);
    expect(elemento.querySelector('[role="alert"]')).toBeNull();
  });

  it('al volver a preguntar no queda el error anterior', () => {
    accion.mockReturnValue(throwError(() => new Error('sin conexión')));
    si().click();
    fixture.detectChanges();
    no().click();

    fixture.componentInstance.preguntar(pregunta);
    fixture.detectChanges();

    expect(elemento.querySelector('[role="alert"]')).toBeNull();
  });
});
