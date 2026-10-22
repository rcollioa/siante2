package siante2.compartido.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import siante2.compartido.domain.SolicitudInvalidaException;

/** Controlador solo de tests: prueba la configuración web transversal sin módulos de negocio. */
@RestController
@RequestMapping("/prueba")
class ControladorPrueba {

    record Respuesta(String valor) {
    }

    @GetMapping
    Respuesta obtener() {
        return new Respuesta("ok");
    }

    @PostMapping
    Respuesta crear() {
        return new Respuesta("creado");
    }

    @GetMapping("/invalida")
    Respuesta invalida() {
        throw new SolicitudInvalidaException("Año fuera de rango");
    }

    @GetMapping("/argumento-inesperado")
    Respuesta argumentoInesperado() {
        throw new IllegalArgumentException("Falla de una librería");
    }

    @GetMapping("/base-caida")
    Respuesta baseCaida() {
        throw new DataAccessResourceFailureException("ORA-12541");
    }

    @GetMapping("/sin-conexion")
    Respuesta sinConexion() {
        throw new CannotCreateTransactionException("Hikari sin conexiones");
    }

    record Entrada(@NotBlank(message = "es obligatorio") String nombre) {
    }

    @PostMapping("/cuerpo")
    Respuesta conCuerpo(@Valid @RequestBody Entrada entrada) {
        return new Respuesta(entrada.nombre());
    }

    @GetMapping("/anio")
    Respuesta conParametro(@RequestParam @Min(value = 2000, message = "debe ser 2000 o posterior") int anio) {
        return new Respuesta(String.valueOf(anio));
    }

    @GetMapping("/bug")
    Respuesta bug() {
        throw new IllegalStateException("detalle interno que no debe llegar al cliente");
    }
}
