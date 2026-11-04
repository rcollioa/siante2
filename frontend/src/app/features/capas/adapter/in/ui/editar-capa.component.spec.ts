import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { CategoriasApiService } from '../../../../categorias/categorias-api.service';
import { MisCapas } from '../../../application/mis-capas';
import { CapaNoEncontrada, CapaPropia, EdicionCapa } from '../../../domain/capa';
import { EditarCapaComponent } from './editar-capa.component';

describe('EditarCapaComponent', () => {
  const bodegas: CapaPropia = { id: 7, nombre: 'Bodegas', descripcion: 'Región', visibilidad: 'PRIVADA', categoriaId: 3 };
  const categorias = [
    { id: 3, nombre: 'Agua', descripcion: '' },
    { id: 4, nombre: 'Salud', descripcion: '' }
  ];
  const editar = vi.fn<(id: number, edicion: EdicionCapa) => Observable<void>>();
  const listar = vi.fn<CategoriasApiService['listar']>();
  let fixture: ComponentFixture<EditarCapaComponent>;
  let elemento: HTMLElement;

  const dialogo = () => elemento.querySelector('dialog') as HTMLDialogElement;
  const nombre = () => elemento.querySelector('#nombre-capa') as HTMLInputElement;
  const descripcion = () => elemento.querySelector('#descripcion-capa') as HTMLTextAreaElement;
  const categoria = () => elemento.querySelector('#categoria-capa') as HTMLSelectElement;
  const radio = (valor: string) => elemento.querySelector(`input[value="${valor}"]`) as HTMLInputElement;
  const aplicar = () => elemento.querySelector('button[type="submit"]') as HTMLButtonElement;
  const errores = () => Array.from(elemento.querySelectorAll('.error')).map((e) => e.textContent?.trim());

  function escribir(campo: HTMLInputElement | HTMLTextAreaElement, valor: string): void {
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function elegir(valor: string): void {
    categoria().value = valor;
    categoria().dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  /** Enviar el formulario como lo hace el botón, que jsdom no dispara con un clic. */
  function guardar(): void {
    (elemento.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  function abrir(capa: CapaPropia = bodegas): void {
    fixture.componentInstance.abrir(capa);
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
    editar.mockReset();
    editar.mockReturnValue(of(undefined));
    listar.mockReset();
    listar.mockReturnValue(of(categorias));
    TestBed.configureTestingModule({
      providers: [
        { provide: MisCapas, useValue: { editar } },
        { provide: CategoriasApiService, useValue: { listar } }
      ]
    });
    fixture = TestBed.createComponent(EditarCapaComponent);
    fixture.detectChanges();
    elemento = fixture.nativeElement;
  });

  it('abre con los datos de la capa, sin mostrar errores', () => {
    abrir();

    expect(dialogo().open).toBe(true);
    expect(nombre().value).toBe('Bodegas');
    expect(descripcion().value).toBe('Región');
    expect(categoria().value).toBe('3');
    expect(radio('PRIVADA').checked).toBe(true);
    expect(radio('PUBLICA').checked).toBe(false);
    expect(errores()).toEqual([]);
    expect(document.activeElement).toBe(nombre());
  });

  it('guarda lo elegido y cierra', () => {
    abrir();

    escribir(nombre(), 'Bodegas norte');
    escribir(descripcion(), 'Antofagasta');
    elegir('4');
    radio('PUBLICA').click();
    guardar();

    expect(editar).toHaveBeenCalledWith(7, { nombre: 'Bodegas norte', descripcion: 'Antofagasta', categoriaId: 4, visibilidad: 'PUBLICA' });
    expect(dialogo().open).toBe(false);
  });

  it('si nada cambió cierra sin escribir, como SIATE', () => {
    abrir();

    escribir(nombre(), '  Bodegas ');
    guardar();

    expect(editar).not.toHaveBeenCalled();
    expect(dialogo().open).toBe(false);
  });

  it('con datos inválidos muestra los errores y no escribe', () => {
    abrir();

    escribir(nombre(), 'ab');
    guardar();

    expect(errores()).toEqual(['El nombre debe tener al menos 4 caracteres.']);
    expect(nombre().getAttribute('aria-invalid')).toBe('true');
    expect(nombre().getAttribute('aria-describedby')).toBe('error-nombre-capa');
    expect(editar).not.toHaveBeenCalled();
    expect(dialogo().open).toBe(true);
  });

  it('al salir del nombre ya muestra si falta', () => {
    abrir();

    escribir(nombre(), '');
    expect(errores()).toEqual([]);

    nombre().dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    expect(errores()).toEqual(['Nombre requerido.']);
  });

  it('una categoría que ya no está activa obliga a elegir otra', () => {
    abrir({ ...bodegas, categoriaId: 99 });

    expect(categoria().value).toBe('0');

    guardar();
    expect(errores()).toEqual(['Categoría requerida.']);
    expect(editar).not.toHaveBeenCalled();

    elegir('4');
    guardar();
    expect(editar).toHaveBeenCalledWith(7, expect.objectContaining({ categoriaId: 4 }));
  });

  it('sin categorías avisa, no guarda y deja reintentar', () => {
    listar.mockReturnValue(throwError(() => new Error('sin conexión')));
    abrir();

    expect(errores()).toEqual([expect.stringContaining('No se pudieron cargar las categorías.')]);
    escribir(nombre(), 'Bodegas norte');
    guardar();
    expect(editar).not.toHaveBeenCalled();

    listar.mockReturnValue(of(categorias));
    (elemento.querySelector('button.enlace') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(errores()).toEqual([]);
    expect(categoria().value).toBe('3');
  });

  it('mientras guarda no deja enviar dos veces', () => {
    const respuesta = new Subject<void>();
    editar.mockReturnValue(respuesta);
    abrir();
    escribir(nombre(), 'Bodegas norte');

    guardar();
    expect(aplicar().disabled).toBe(true);
    expect(aplicar().textContent).toContain('Guardando');

    guardar();
    expect(editar).toHaveBeenCalledOnce();

    respuesta.next();
    expect(dialogo().open).toBe(false);
  });

  it('si la capa ya no existe lo explica y queda abierto', () => {
    editar.mockReturnValue(throwError(() => new CapaNoEncontrada(7)));
    abrir();
    escribir(nombre(), 'Bodegas norte');

    guardar();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toBe('La capa ya no existe o no tiene permiso para modificarla.');
    expect(dialogo().open).toBe(true);
    expect(aplicar().disabled).toBe(false);
  });

  it('Cancelar cierra sin guardar y al volver a abrir parte de nuevo', () => {
    abrir();
    escribir(nombre(), 'ab');
    guardar();

    (elemento.querySelector('.botones button[type="button"]') as HTMLButtonElement).click();
    expect(dialogo().open).toBe(false);
    expect(editar).not.toHaveBeenCalled();

    abrir();
    expect(nombre().value).toBe('Bodegas');
    expect(errores()).toEqual([]);
  });
});
