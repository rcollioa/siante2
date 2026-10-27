package siante2.compartido.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/** Controlador solo de tests: un catálogo ({@link Catalogo}) para probar su política de caché. */
@RestController
@Catalogo
@RequestMapping("/prueba/catalogo")
class CatalogoPrueba {

    @GetMapping
    ControladorPrueba.Respuesta obtener() {
        return new ControladorPrueba.Respuesta("catálogo");
    }

    /** Como un endpoint de archivos: pide no calcular ETag para no copiar el contenido en memoria. */
    @GetMapping("/archivo")
    byte[] archivo(HttpServletRequest request) {
        ShallowEtagHeaderFilter.disableContentCaching(request);
        return new byte[] {1, 2, 3};
    }
}
