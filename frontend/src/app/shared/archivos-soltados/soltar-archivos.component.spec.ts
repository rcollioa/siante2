import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ArchivosSoltados } from './archivos-soltados.service';
import { SoltarArchivosComponent } from './soltar-archivos.component';

describe('SoltarArchivosComponent', () => {
  const archivo = { name: 'bodegas.kml' } as File;
  let fixture: ComponentFixture<SoltarArchivosComponent>;
  let soltados: ArchivosSoltados;
  let dialogoAbierto: HTMLDialogElement | null;

  const aviso = () => (fixture.nativeElement as HTMLElement).querySelector('.aviso');

  /** jsdom no trae DragEvent: basta con lo que el componente lee del evento. */
  function disparar(tipo: string, datos: { tipos?: string[]; archivos?: File[]; destino?: Element | null } = {}): Event & { dataTransfer: { dropEffect: string } } {
    const evento = new Event(tipo, { cancelable: true });
    Object.defineProperty(evento, 'dataTransfer', {
      value: { types: datos.tipos ?? ['Files'], files: datos.archivos ?? [], dropEffect: 'none' }
    });
    Object.defineProperty(evento, 'relatedTarget', { value: datos.destino ?? null });
    document.dispatchEvent(evento);
    fixture.detectChanges();
    return evento as Event & { dataTransfer: { dropEffect: string } };
  }

  function abrirDialogo(recibeArchivos: boolean): void {
    dialogoAbierto = document.createElement('dialog');
    dialogoAbierto.setAttribute('open', '');
    if (recibeArchivos) {
      dialogoAbierto.setAttribute('data-recibe-archivos', '');
    }
    document.body.append(dialogoAbierto);
  }

  beforeEach(() => {
    dialogoAbierto = null;
    fixture = TestBed.createComponent(SoltarArchivosComponent);
    soltados = TestBed.inject(ArchivosSoltados);
    fixture.detectChanges();
  });

  afterEach(() => dialogoAbierto?.remove());

  it('al arrastrar un archivo cubre la ventana e impide que el navegador lo abra', () => {
    const evento = disparar('dragover');

    expect(aviso()?.textContent).toContain('Suelte el archivo para crear una capa');
    expect(evento.defaultPrevented).toBe(true);
    expect(evento.dataTransfer.dropEffect).toBe('copy');
    expect(soltados.arrastrando()).toBe(true);
  });

  it('el aviso sigue al pasar de un elemento a otro y se quita al salir de la ventana', () => {
    disparar('dragenter');

    disparar('dragleave', { destino: document.body });
    expect(aviso()).not.toBeNull();

    disparar('dragleave');
    expect(aviso()).toBeNull();
    expect(soltados.arrastrando()).toBe(false);
  });

  it('al soltar entrega los archivos y quita el aviso', () => {
    disparar('dragover');

    const evento = disparar('drop', { archivos: [archivo] });

    expect(evento.defaultPrevented).toBe(true);
    expect(soltados.pendientes()).toEqual([archivo]);
    expect(aviso()).toBeNull();
    expect(soltados.arrastrando()).toBe(false);
  });

  it('soltar algo sin archivos no entrega nada', () => {
    disparar('dragover');

    disparar('drop');

    expect(soltados.pendientes()).toEqual([]);
  });

  it('arrastrar un texto no es asunto suyo', () => {
    const encima = disparar('dragover', { tipos: ['text/plain'] });
    const soltado = disparar('drop', { tipos: ['text/plain'] });

    expect(aviso()).toBeNull();
    expect(encima.defaultPrevented).toBe(false);
    expect(soltado.defaultPrevented).toBe(false);
  });

  it('arrastrar una imagen de la propia página no cuenta como un archivo', () => {
    disparar('dragstart');

    disparar('dragover');
    disparar('drop', { archivos: [archivo] });
    expect(aviso()).toBeNull();
    expect(soltados.pendientes()).toEqual([]);

    disparar('dragend');
    disparar('dragover');
    expect(aviso()).not.toBeNull();
  });

  it('con otro diálogo abierto no recibe el archivo, y tampoco deja que el navegador lo abra', () => {
    abrirDialogo(false);

    const encima = disparar('dragover');
    expect(encima.defaultPrevented).toBe(true);
    expect(encima.dataTransfer.dropEffect).toBe('none');
    expect(aviso()).toBeNull();
    expect(soltados.arrastrando()).toBe(false);

    const soltado = disparar('drop', { archivos: [archivo] });
    expect(soltado.defaultPrevented).toBe(true);
    expect(soltados.pendientes()).toEqual([]);
  });

  it('con el diálogo que recibe archivos abierto lo entrega sin cubrir la ventana', () => {
    abrirDialogo(true);

    disparar('dragover');
    expect(aviso()).toBeNull();
    expect(soltados.arrastrando()).toBe(true);

    disparar('drop', { archivos: [archivo] });
    expect(soltados.pendientes()).toEqual([archivo]);
  });
});
