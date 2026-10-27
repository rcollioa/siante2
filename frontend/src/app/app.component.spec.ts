import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  it('muestra el contenido recién cuando el router revisó las guardas de la primera ruta', async () => {
    const guarda = new Subject<boolean>();
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([{ path: '**', canActivate: [() => guarda], children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    const fixture = TestBed.createComponent(AppComponent);
    const contenido = (): HTMLElement => fixture.nativeElement.querySelector('main');

    const navegacion = TestBed.inject(Router).navigateByUrl('/mapa');
    await new Promise((listo) => setTimeout(listo));
    fixture.detectChanges();
    expect(contenido().classList).toContain('esperando');

    guarda.next(true);
    guarda.complete();
    await navegacion;
    fixture.detectChanges();
    expect(contenido().classList).not.toContain('esperando');
  });
});
