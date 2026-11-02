import { ChangeDetectionStrategy, Component, ElementRef, signal, viewChild } from '@angular/core';
import { Observable } from 'rxjs';

export interface Pregunta {
  mensaje: string;
  /** Sobre qué se pregunta, por ejemplo el nombre de la capa. */
  detalle: string;
  /** Lo que se hace al responder "Sí". */
  accion: () => Observable<unknown>;
  mensajeDeError: (error: unknown) => string;
}

/** El modal "Confirmación" de SIATE, con Sí y No. Si la acción falla, queda abierto con el error para reintentar. "No"
 * va primero: es el que recibe el foco, para que un Enter de más no borre nada. */
@Component({
  selector: 'app-confirmacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog #dialogo aria-labelledby="titulo-confirmacion" aria-describedby="mensaje-confirmacion">
      <h2 id="titulo-confirmacion">Confirmación</h2>
      <p id="mensaje-confirmacion">{{ pregunta()?.mensaje }}</p>
      <p class="detalle">{{ pregunta()?.detalle }}</p>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <div class="botones">
        <button type="button" class="no" (click)="cerrar()">No</button>
        <button type="button" class="si" [disabled]="enviando()" (click)="confirmar()">
          {{ enviando() ? 'Guardando…' : 'Sí' }}
        </button>
      </div>
    </dialog>
  `,
  styles: `
    dialog {
      box-sizing: border-box;
      width: min(26rem, calc(100vw - 2rem));
      padding: 1rem;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      color: var(--color-ink);
      box-shadow: var(--shadow-md);
    }

    dialog::backdrop {
      background: rgb(26 32 51 / 0.45);
    }

    h2 {
      margin: 0 0 0.5rem;
      font-size: 1.1rem;
    }

    p {
      margin: 0 0 0.5rem;
    }

    .detalle {
      color: var(--color-muted);
      font-size: 0.875rem;
      overflow-wrap: anywhere;
    }

    .error {
      color: var(--color-danger);
      font-size: 0.875rem;
    }

    .botones {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      margin-top: 0.75rem;
    }

    button {
      min-width: 4.5rem;
      padding: 0.4rem 0.9rem;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      color: var(--color-ink);
      font: inherit;
      cursor: pointer;
    }

    .no:hover {
      background: var(--color-hover);
    }

    .si {
      border-color: var(--color-primary-dark);
      background: var(--color-primary-dark);
      color: var(--color-surface);
    }

    .si:disabled {
      cursor: progress;
      opacity: 0.7;
    }

    button:focus-visible {
      outline: 2px solid var(--color-primary);
      outline-offset: 2px;
    }
  `
})
export class ConfirmacionComponent {
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  protected readonly pregunta = signal<Pregunta | null>(null);
  protected readonly enviando = signal(false);
  protected readonly error = signal('');

  preguntar(pregunta: Pregunta): void {
    this.pregunta.set(pregunta);
    this.error.set('');
    this.dialogo().nativeElement.showModal();
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected confirmar(): void {
    const pregunta = this.pregunta();
    if (!pregunta || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set('');
    pregunta.accion().subscribe({
      next: () => {
        this.enviando.set(false);
        this.cerrar();
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.error.set(pregunta.mensajeDeError(error));
      }
    });
  }
}
