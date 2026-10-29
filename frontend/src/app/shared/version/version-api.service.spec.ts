import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { VersionApiService } from './version-api.service';

describe('VersionApiService', () => {
  it('pide la versión del backend', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    let recibida: unknown;

    TestBed.inject(VersionApiService).obtener().subscribe((version) => (recibida = version));
    http.expectOne('/siante2/api/version').flush({ version: '1.2.0', compilada: '2026-10-29T17:28:03Z' });

    expect(recibida).toEqual({ version: '1.2.0', compilada: '2026-10-29T17:28:03Z' });
    http.verify();
  });
});
