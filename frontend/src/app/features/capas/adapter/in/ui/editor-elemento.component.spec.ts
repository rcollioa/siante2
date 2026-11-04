import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CatalogoIconos } from '../../../application/catalogo-iconos';
import { EdicionElementos } from '../../../application/edicion-elementos';
import { ElementoCapa } from '../../../domain/capa';
import { EditorElementoComponent } from './editor-elemento.component';

describe('EditorElementoComponent', () => {
  const sinEstilo = { color: '', grosor: 0, relleno: '', icono: '' };
  const poligono: ElementoCapa = {
    id: 3,
    geometria: { type: 'Polygon', coordinates: [] },
    radio: 0,
    nombre: 'Terreno',
    descripcion: 'Al sur',
    estilo: sinEstilo,
    metadatos: [{ nombre: 'Fuente', valor: 'Terreno' }]
  };
  const punto: ElementoCapa = { ...poligono, geometria: { type: 'Point', coordinates: [-70, -33] } };
  const linea: ElementoCapa = { ...poligono, geometria: { type: 'LineString', coordinates: [] } };
  const circulo: ElementoCapa = { ...punto, radio: 500 };
  const icono = { id: 1, nombre: 'Bodega', archivo: '', imagen: 'data:image/png;base64,AA==', propio: false };
  const cambiarDatos = vi.fn();
  let fixture: ComponentFixture<EditorElementoComponent>;
  let elemento: HTMLElement;

  const campo = (id: string) => elemento.querySelector(`#${id}`) as HTMLInputElement;

  function mostrar(editado: ElementoCapa): void {
    fixture.componentRef.setInput('elemento', editado);
    fixture.detectChanges();
  }

  function escribir(entrada: HTMLInputElement | HTMLTextAreaElement, valor: string, evento = 'input'): void {
    entrada.value = valor;
    entrada.dispatchEvent(new Event(evento));
  }

  beforeEach(() => {
    cambiarDatos.mockReset();
    TestBed.configureTestingModule({
      providers: [
        { provide: EdicionElementos, useValue: { cambiarDatos } },
        { provide: CatalogoIconos, useValue: { estado: signal('listo'), delSistema: signal([icono]), propios: signal([]), cargar: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(EditorElementoComponent);
    elemento = fixture.nativeElement;
  });

  it('muestra los datos del elemento, y sin nombre lo dice', () => {
    mostrar(poligono);
    expect(elemento.querySelector('h2')?.textContent).toContain('Terreno');
    expect(campo('nombre-elemento').value).toBe('Terreno');
    expect(campo('descripcion-elemento').value).toBe('Al sur');

    mostrar({ ...poligono, nombre: '  ' });
    expect(elemento.querySelector('h2')?.textContent).toContain('Elemento sin nombre');
  });

  it('cada cambio de nombre y descripción llega a la edición', () => {
    mostrar(poligono);

    escribir(campo('nombre-elemento'), 'Terreno norte');
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, { nombre: 'Terreno norte' });

    escribir(campo('descripcion-elemento'), 'Al norte');
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, { descripcion: 'Al norte' });
  });

  it('un marcador elige su ícono del catálogo, que cambia su estilo; otra forma no lo ofrece', () => {
    mostrar(punto);
    (elemento.querySelector('button[aria-label="Bodega"]') as HTMLButtonElement).click();

    expect(cambiarDatos).toHaveBeenCalledWith(punto, { estilo: { ...sinEstilo, icono: icono.imagen } });

    mostrar(linea);
    expect(elemento.querySelector('app-selector-icono')).toBeNull();
  });

  it('muestra los colores de la aplicación si el elemento no define los suyos', () => {
    mostrar(poligono);

    expect(campo('color-elemento').value).toBe('#71aecb');
    expect(campo('relleno-elemento').value).toBe('#e6f8fd');
    expect(campo('grosor-elemento').value).toBe('2');
  });

  it('muestra los colores guardados, también si son rgba', () => {
    mostrar({ ...poligono, estilo: { ...sinEstilo, color: 'rgb(255, 0, 0)', relleno: '#00ff00', grosor: 5 } });

    expect(campo('color-elemento').value).toBe('#ff0000');
    expect(campo('relleno-elemento').value).toBe('#00ff00');
    expect(campo('grosor-elemento').value).toBe('5');
  });

  it('el color de fondo queda semitransparente, como en SIATE', () => {
    mostrar(poligono);

    escribir(campo('relleno-elemento'), '#ff0000', 'change');

    expect(cambiarDatos).toHaveBeenCalledWith(poligono, { estilo: { ...sinEstilo, relleno: 'rgba(255,0,0,0.5)' } });
  });

  it('cambia el color de línea y el grosor, e ignora un grosor fuera de rango', () => {
    mostrar(poligono);

    escribir(campo('color-elemento'), '#ff0000', 'change');
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, { estilo: { ...sinEstilo, color: '#ff0000' } });

    escribir(campo('grosor-elemento'), '4');
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, { estilo: { ...sinEstilo, grosor: 4 } });

    cambiarDatos.mockClear();
    escribir(campo('grosor-elemento'), '0');
    expect(cambiarDatos).not.toHaveBeenCalled();
  });

  it('un punto no tiene estilos de línea; una línea no tiene fondo; un círculo sí', () => {
    mostrar(punto);
    expect(campo('color-elemento')).toBeNull();

    mostrar(linea);
    expect(campo('color-elemento')).not.toBeNull();
    expect(campo('relleno-elemento')).toBeNull();

    mostrar(circulo);
    expect(campo('relleno-elemento')).not.toBeNull();
  });

  it('agrega, cambia y quita metadatos', () => {
    mostrar(poligono);
    const filas = () => elemento.querySelectorAll('.metadato');
    expect(filas().length).toBe(1);

    (elemento.querySelector('.agregar') as HTMLButtonElement).click();
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, {
      metadatos: [poligono.metadatos[0], { nombre: '', valor: '' }]
    });

    const [nombre, valor] = Array.from(filas()[0].querySelectorAll('input'));
    escribir(nombre, 'Origen');
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, { metadatos: [{ nombre: 'Origen', valor: 'Terreno' }] });
    escribir(valor, 'Oficina');
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, { metadatos: [{ nombre: 'Fuente', valor: 'Oficina' }] });

    (filas()[0].querySelector('.quitar') as HTMLButtonElement).click();
    expect(cambiarDatos).toHaveBeenLastCalledWith(poligono, { metadatos: [] });
  });
});
