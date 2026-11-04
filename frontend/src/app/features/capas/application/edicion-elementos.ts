import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { ElementoMapa, FormaElemento, HerramientaDibujo } from '../../mapa/domain/mapa';
import {
  aplicar,
  CambiosElementos,
  cantidadDe,
  conCambio,
  conNuevo,
  elementoNuevo,
  limpios,
  sinElemento,
  SIN_CAMBIOS
} from '../domain/cambios-elementos';
import { CambiosRechazados, Capa, CapaNoEncontrada, ElementoCapa } from '../domain/capa';
import { CapasAbiertas } from './capas-abiertas';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** Lo que se puede cambiar de un elemento sin tocar el mapa. */
export type DatosElemento = Partial<Pick<ElementoCapa, 'nombre' | 'descripcion' | 'estilo' | 'metadatos'>>;

/** El usuario pidió salir de la edición y hay cambios sin guardar. Cada pedido es otro objeto, aunque repita lo mismo. */
export interface SalidaPedida {
  despues: () => void;
}

/** Dibujar y editar los elementos de una capa propia, una a la vez. Como en SIATE, nada se escribe hasta "Guardar", pero
 * aquí se guarda todo junto: si falla, los cambios siguen pendientes y no quedan a medias. */
@Injectable({ providedIn: 'root' })
export class EdicionElementos {
  private readonly repositorio = inject(CapasRepositorio);
  private readonly capasAbiertas = inject(CapasAbiertas);
  private readonly mapa = inject(MapaPort);
  private readonly capaId = signal<number | null>(null);
  private readonly pendientes = signal<CambiosElementos>(SIN_CAMBIOS);
  private readonly herramientaActual = signal<HerramientaDibujo | null>(null);
  private readonly enviando = signal(false);
  private readonly mensajeError = signal('');
  private readonly recienGuardado = signal(false);
  private readonly salida = signal<SalidaPedida | null>(null);
  private siguienteProvisional = -1;

  /** Null también si la capa se cerró o se eliminó mientras se editaba. */
  readonly capa = computed<Capa | null>(
    () => this.capasAbiertas.abiertas().find(({ capa }) => capa.id === this.capaId())?.capa ?? null
  );
  readonly herramienta = this.herramientaActual.asReadonly();
  readonly guardando = this.enviando.asReadonly();
  readonly error = this.mensajeError.asReadonly();
  readonly guardado = this.recienGuardado.asReadonly();
  readonly salidaPedida = this.salida.asReadonly();
  readonly cantidadDeCambios = computed(() => (this.capa() ? cantidadDe(this.pendientes()) : 0));
  readonly hayCambios = computed(() => this.cantidadDeCambios() > 0);
  /** El elemento elegido, si es de la capa en edición: los de otras capas abiertas solo se pueden mirar. */
  readonly seleccionado = computed<ElementoCapa | null>(() => {
    const elegido = this.capasAbiertas.elegido();
    return elegido && elegido.capa.id === this.capaId() ? elegido.elemento : null;
  });

  constructor() {
    // Activar la regla ya soltó la herramienta en el mapa: la barra no debe seguir mostrándola elegida.
    effect(() => {
      if (this.mapa.midiendo()) {
        this.herramientaActual.set(null);
      }
    });
  }

  /** Abre la capa si hace falta y la deja lista para dibujar. Si otra tenía cambios sin guardar, primero se pregunta. */
  iniciar(id: number): void {
    if (this.capa()?.id === id) {
      this.mostrarCapa(id);
      return;
    }
    this.terminar(() => this.capasAbiertas.abrir(id, (capa) => this.editar(capa)));
  }

  /** Deja de editar y sigue con `despues`. Con cambios sin guardar, antes se pide la confirmación (`salidaPedida`). */
  terminar(despues: () => void = () => undefined): void {
    if (this.hayCambios()) {
      this.salida.set({ despues });
      return;
    }
    this.dejarDeEditar();
    despues();
  }

  /** La respuesta a `salidaPedida`: la capa vuelve a lo que estaba guardado. */
  descartarYSalir(): void {
    const pedida = this.salida();
    const capa = this.capa();
    this.salida.set(null);
    if (capa) {
      this.mapa.mostrarCapa(capa.id, capa.elementos);
    }
    this.dejarDeEditar();
    pedida?.despues();
  }

  /** La misma herramienta otra vez la suelta, como en SIATE. */
  usarHerramienta(herramienta: HerramientaDibujo): void {
    const capa = this.capa();
    if (capa) {
      this.mostrarCapa(capa.id);
      this.elegirHerramienta(this.herramientaActual() === herramienta ? null : herramienta);
    }
  }

  soltarHerramienta(): void {
    if (this.herramientaActual()) {
      this.elegirHerramienta(null);
    }
  }

  cambiarDatos(elemento: ElementoCapa, datos: DatosElemento): void {
    this.reemplazar(elemento, datos);
  }

  eliminarSeleccionado(): void {
    const capa = this.capa();
    const elemento = this.seleccionado();
    if (capa && elemento) {
      this.registrar(sinElemento(this.pendientes(), elemento));
      this.mapa.quitarElemento(capa.id, elemento);
    }
  }

  guardar(): void {
    const capa = this.capa();
    if (!capa || !this.hayCambios() || this.enviando()) {
      return;
    }
    const enviados = this.pendientes();
    const cuerpo = limpios(enviados);
    this.enviando.set(true);
    this.mensajeError.set('');
    this.repositorio.guardarElementos(capa.id, cuerpo).subscribe({
      next: (idsNuevos) => {
        this.enviando.set(false);
        // El mapa pasa a mostrar lo guardado: los nuevos ya con su ID, y sin lo que se limpió al enviar.
        enviados.nuevos.forEach((elemento, indice) => {
          const guardado: ElementoCapa = { ...cuerpo.nuevos[indice], id: idsNuevos[indice] };
          this.mapa.reemplazarElemento(capa.id, elemento, guardado);
        });
        enviados.modificados.forEach((elemento, indice) =>
          this.mapa.reemplazarElemento(capa.id, elemento, cuerpo.modificados[indice])
        );
        this.capasAbiertas.actualizar(capa.id, { elementos: aplicar(capa.elementos, cuerpo, idsNuevos) });
        this.pendientes.set(SIN_CAMBIOS);
        this.recienGuardado.set(true);
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.mensajeError.set(mensaje(error));
      }
    });
  }

  private editar(capa: Capa): void {
    if (!capa.editable) {
      this.capasAbiertas.avisarError('En esta capa no se puede dibujar: sus elementos vienen de un servidor o no es suya.');
      return;
    }
    this.mapa.editarCapa(capa.id, {
      alDibujar: (forma) => this.alDibujar(capa.id, forma),
      alCambiarForma: (elemento, forma) => this.alCambiarForma(elemento, forma)
    });
    this.capaId.set(capa.id);
    this.pendientes.set(SIN_CAMBIOS);
    this.mensajeError.set('');
    this.recienGuardado.set(false);
  }

  private dejarDeEditar(): void {
    this.mapa.terminarEdicion();
    this.capaId.set(null);
    this.herramientaActual.set(null);
    this.pendientes.set(SIN_CAMBIOS);
    this.mensajeError.set('');
    this.recienGuardado.set(false);
  }

  private alDibujar(capaId: number, forma: FormaElemento): void {
    const elemento = elementoNuevo(this.siguienteProvisional--, forma);
    this.registrar(conNuevo(this.pendientes(), elemento));
    this.mostrarCapa(capaId);
    this.mapa.agregarElemento(capaId, elemento);
  }

  /** El mapa solo tiene los elementos que recibió de aquí, que son de capa. */
  private alCambiarForma(elemento: ElementoMapa, forma: FormaElemento): void {
    this.reemplazar(elemento as ElementoCapa, forma);
  }

  private reemplazar(elemento: ElementoCapa, cambio: Partial<ElementoCapa>): void {
    const capa = this.capa();
    if (capa) {
      const nuevo = { ...elemento, ...cambio };
      this.registrar(conCambio(this.pendientes(), nuevo));
      this.mapa.reemplazarElemento(capa.id, elemento, nuevo);
    }
  }

  private registrar(cambios: CambiosElementos): void {
    this.pendientes.set(cambios);
    this.recienGuardado.set(false);
    this.mensajeError.set('');
  }

  private elegirHerramienta(herramienta: HerramientaDibujo | null): void {
    this.herramientaActual.set(herramienta);
    this.mapa.usarHerramienta(herramienta);
  }

  /** Dibujar en una capa oculta no se vería. */
  private mostrarCapa(id: number): void {
    if (this.capasAbiertas.abiertas().some(({ capa, visible }) => capa.id === id && !visible)) {
      this.capasAbiertas.alternarVisibilidad(id);
    }
  }
}

function mensaje(error: unknown): string {
  if (error instanceof CapaNoEncontrada) {
    return 'La capa ya no existe o no tiene permiso para modificarla.';
  }
  return error instanceof CambiosRechazados ? error.message : 'No se pudieron guardar los cambios. Intente nuevamente.';
}
