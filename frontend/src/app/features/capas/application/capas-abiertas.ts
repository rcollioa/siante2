import { computed, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, map, of, Subscription } from 'rxjs';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import {
  Capa,
  CapaNoEncontrada,
  ElementoCapa,
  ElementoDeServicio,
  extensionDe,
  ServidorDeMapasNoDisponible
} from '../domain/capa';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** Una capa abierta en el mapa y si se está mostrando. */
export interface CapaEnMapa {
  capa: Capa;
  visible: boolean;
}

/** El elemento en que el usuario hizo clic y la capa abierta a la que pertenece. */
export interface ElementoElegido {
  capa: Capa;
  elemento: ElementoCapa;
}

/** Lo que respondió el servidor de una capa WMS abierta al consultar un punto. */
export interface ResultadoWms {
  capa: Capa;
  elementos: readonly ElementoDeServicio[];
}

/** El punto del mapa consultado a las capas WMS: solo las capas con algo en ese punto, y las que no respondieron. */
export interface PuntoConsultado {
  consultando: boolean;
  resultados: readonly ResultadoWms[];
  sinRespuesta: readonly Capa[];
}

interface RespuestaWms {
  capaId: number;
  elementos: readonly ElementoDeServicio[];
  fallo: boolean;
}

/** Las capas abiertas en el mapa, sobrepuestas como en SIATE: cada una se puede ocultar, acercar o cerrar. */
@Injectable({ providedIn: 'root' })
export class CapasAbiertas {
  private readonly repositorio = inject(CapasRepositorio);
  private readonly mapa = inject(MapaPort);
  private readonly pedidos = new Map<number, Subscription>();
  private readonly alAbrirse = new Map<number, (capa: Capa) => void>();
  private readonly lista = signal<readonly CapaEnMapa[]>([]);
  private readonly idsAbriendo = signal<ReadonlySet<number>>(new Set());
  private readonly mensajeError = signal('');

  /** En el orden en que se abrieron: la última queda encima en el mapa. */
  readonly abiertas = this.lista.asReadonly();
  /** IDs de las capas que se están descargando. */
  readonly abriendo = this.idsAbriendo.asReadonly();
  readonly error = this.mensajeError.asReadonly();
  /** Capas WMS abiertas cuyo servidor no respondió al abrirlas o no está entregando imágenes. */
  readonly sinRespuesta = computed(() => {
    const ids = this.mapa.capasSinRespuesta();
    return this.lista()
      .filter(({ capa }) => capa.servicio?.disponible === false || ids.has(capa.id))
      .map(({ capa }) => capa);
  });
  /** Los elementos en el mapa son solo los de las capas abiertas, así que lo seleccionado es de una de ellas. */
  readonly elegido = computed<ElementoElegido | null>(() => {
    const seleccion = this.mapa.elementoSeleccionado();
    const abierta = seleccion && this.lista().find(({ capa }) => capa.id === seleccion.capa);
    return abierta ? { capa: abierta.capa, elemento: seleccion.elemento as ElementoCapa } : null;
  });

  /** Un clic en el mapa pregunta a cada capa WMS visible; otro clic cancela lo que faltaba y vuelve a preguntar. Un
   * servidor que no responde no impide ver lo que respondieron los demás. */
  private readonly respuestasWms = rxResource({
    params: () => {
      const puntos = this.mapa.consultaWms();
      return puntos.length > 0 ? puntos : undefined;
    },
    stream: ({ params: puntos }) =>
      forkJoin(
        puntos.map((punto) =>
          this.repositorio.consultarPunto(punto).pipe(
            map((elementos): RespuestaWms => ({ capaId: punto.capa, elementos, fallo: false })),
            catchError(() => of<RespuestaWms>({ capaId: punto.capa, elementos: [], fallo: true }))
          )
        )
      )
  });
  /** Lo que hay en el punto en que el usuario hizo clic, según los servidores de las capas WMS abiertas; null si no
   * se consultó ninguna. */
  readonly consultado = computed<PuntoConsultado | null>(() => {
    if (this.mapa.consultaWms().length === 0) {
      return null;
    }
    const respuestas = this.respuestasWms.value() ?? [];
    const abierta = (id: number) => this.lista().find(({ capa }) => capa.id === id)?.capa;
    return {
      consultando: this.respuestasWms.isLoading(),
      resultados: respuestas.flatMap(({ capaId, elementos }) => {
        const capa = abierta(capaId);
        return capa && elementos.length > 0 ? [{ capa, elementos }] : [];
      }),
      sinRespuesta: respuestas.flatMap(({ capaId, fallo }) => {
        const capa = abierta(capaId);
        return capa && fallo ? [capa] : [];
      })
    };
  });

  estaAbierta(id: number): boolean {
    return this.lista().some(({ capa }) => capa.id === id);
  }

  /** Abre la capa sobre las demás y acerca el mapa a ella. Si ya estaba abierta, la muestra y la acerca. `alAbrirse`: lo
   * que sigue cuando ya está en el mapa; si no se pudo abrir, no se llama. */
  abrir(id: number, alAbrirse?: (capa: Capa) => void): void {
    this.mensajeError.set('');
    const abierta = this.lista().find(({ capa }) => capa.id === id);
    if (abierta) {
      this.cambiarVisibilidad(id, true);
      this.mapa.acercarA(id);
      alAbrirse?.(abierta.capa);
      return;
    }
    if (alAbrirse) {
      this.alAbrirse.set(id, alAbrirse);
    }
    if (this.pedidos.has(id)) {
      return;
    }
    this.idsAbriendo.update((ids) => new Set(ids).add(id));
    this.pedidos.set(
      id,
      this.repositorio.ver(id).subscribe({
        next: (capa) => {
          const alAbrirse = this.alAbrirse.get(id);
          this.terminarPedido(id);
          this.lista.update((abiertas) => [...abiertas, { capa, visible: true }]);
          this.mostrar(capa);
          this.mapa.acercarA(id);
          // Como el marcador del buscador en SIATE: desaparece al elegir otra capa.
          this.mapa.quitarMarcador();
          alAbrirse?.(capa);
        },
        // Si falla, las capas que ya estaban abiertas siguen en el mapa.
        error: (error: unknown) => {
          this.terminarPedido(id);
          this.mensajeError.set(mensaje(error));
        }
      })
    );
  }

  cerrar(id: number): void {
    this.pedidos.get(id)?.unsubscribe();
    this.terminarPedido(id);
    this.lista.update((abiertas) => abiertas.filter(({ capa }) => capa.id !== id));
    this.mapa.quitarCapa(id);
  }

  /** El dueño editó la capa mientras estaba abierta: el panel muestra sus datos nuevos, sin volver a descargarla. */
  actualizar(id: number, datos: Partial<Capa>): void {
    this.lista.update((abiertas) =>
      abiertas.map((abierta) => (abierta.capa.id === id ? { ...abierta, capa: { ...abierta.capa, ...datos } } : abierta))
    );
  }

  alternarVisibilidad(id: number): void {
    const abierta = this.lista().find(({ capa }) => capa.id === id);
    if (abierta) {
      this.cambiarVisibilidad(id, !abierta.visible);
    }
  }

  /** Acercarse a una capa oculta no serviría de nada: también la muestra. */
  acercar(id: number): void {
    if (this.estaAbierta(id)) {
      this.cambiarVisibilidad(id, true);
      this.mapa.acercarA(id);
    }
  }

  deseleccionar(): void {
    this.mapa.deseleccionar();
  }

  avisarError(mensaje: string): void {
    this.mensajeError.set(mensaje);
  }

  descartarError(): void {
    this.mensajeError.set('');
  }

  /** Una capa WMS o WMTS como imágenes de su servidor; las demás, con sus elementos (las WFS, todavía sin nada). Una
   * capa cuyo servidor no respondió queda vacía, como en SIATE. */
  private mostrar(capa: Capa): void {
    const servicio = capa.servicio;
    const capasDelServidor = servicio?.capas.map(({ nombre }) => nombre) ?? [];
    if (servicio?.tipo === 'WMS' && servicio.disponible) {
      this.mapa.mostrarCapaWms(capa.id, { url: servicio.urlMapa, capas: capasDelServidor, extension: extensionDe(servicio) });
    } else if (servicio?.tipo === 'WMTS' && servicio.disponible && servicio.grilla) {
      this.mapa.mostrarCapaWmts(capa.id, {
        url: servicio.urlMapa,
        capas: capasDelServidor,
        extension: extensionDe(servicio),
        grilla: servicio.grilla
      });
    } else {
      this.mapa.mostrarCapa(capa.id, capa.tipo === 'URL' ? [] : capa.elementos);
    }
  }

  private cambiarVisibilidad(id: number, visible: boolean): void {
    this.lista.update((abiertas) => abiertas.map((abierta) => (abierta.capa.id === id ? { ...abierta, visible } : abierta)));
    this.mapa.cambiarVisibilidad(id, visible);
  }

  private terminarPedido(id: number): void {
    this.pedidos.delete(id);
    this.alAbrirse.delete(id);
    this.idsAbriendo.update((ids) => {
      const restantes = new Set(ids);
      restantes.delete(id);
      return restantes;
    });
  }
}

function mensaje(error: unknown): string {
  if (error instanceof CapaNoEncontrada) {
    return 'La capa ya no existe o no tiene permiso para verla.';
  }
  return error instanceof ServidorDeMapasNoDisponible
    ? 'El servidor de mapas de la capa no responde. Intente más tarde.'
    : 'No se pudo abrir la capa. Intente nuevamente.';
}
