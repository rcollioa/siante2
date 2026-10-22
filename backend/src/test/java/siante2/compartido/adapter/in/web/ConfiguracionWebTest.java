package siante2.compartido.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prefijo de la API ({@link ApiPrefijoConfig}), caché ({@link CacheControlAdvice}) y errores
 * ({@link ApiExceptionHandler}), sobre {@link ControladorPrueba}. */
@WebMvcTest(ControladorPrueba.class)
class ConfiguracionWebTest {

    private static final String RUTA = "/siante2/api/prueba";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void losControladoresQuedanBajoElPrefijoDeLaApi() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("ok"));
        mockMvc.perform(get("/prueba")).andExpect(status().isNotFound());
    }

    @Test
    void unGetExitosoSeCachea() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, public"));
    }

    @Test
    void unPostNoSeCachea() throws Exception {
        mockMvc.perform(post(RUTA))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void unaSolicitudInvalidaResponde400SinCachear() throws Exception {
        mockMvc.perform(get(RUTA + "/invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Año fuera de rango"))
                .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void unaIllegalArgumentExceptionNoPrevistaTambienResponde400() throws Exception {
        mockMvc.perform(get(RUTA + "/argumento-inesperado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void unaFallaDeLaBaseResponde503() throws Exception {
        mockMvc.perform(get(RUTA + "/base-caida"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATA_SOURCE_UNAVAILABLE"))
                .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void sinConexionAlAbrirLaTransaccionTambienResponde503() throws Exception {
        mockMvc.perform(get(RUTA + "/sin-conexion"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATA_SOURCE_UNAVAILABLE"));
    }

    @Test
    void unCuerpoInvalidoRespondeLosCamposQueFallan() throws Exception {
        mockMvc.perform(post(RUTA + "/cuerpo").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("nombre: es obligatorio"));
    }

    @Test
    void unJsonMalFormadoResponde400() throws Exception {
        mockMvc.perform(post(RUTA + "/cuerpo").contentType(MediaType.APPLICATION_JSON).content("{nombre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es válido."));
    }

    @Test
    void unParametroFueraDeRangoResponde400() throws Exception {
        mockMvc.perform(get(RUTA + "/anio").param("anio", "1999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("anio: debe ser 2000 o posterior"))
                .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void unParametroFaltanteResponde400() throws Exception {
        mockMvc.perform(get(RUTA + "/anio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Falta el parámetro 'anio'."));
    }

    @Test
    void unParametroMalTipadoResponde400() throws Exception {
        mockMvc.perform(get(RUTA + "/anio").param("anio", "dosmil"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("El parámetro 'anio' no tiene un formato válido."));
    }

    @Test
    void unaRutaInexistenteResponde404ConElMismoFormato() throws Exception {
        mockMvc.perform(get(RUTA + "/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unMetodoNoPermitidoResponde405ConCabeceraAllow() throws Exception {
        mockMvc.perform(delete(RUTA))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(header().exists(HttpHeaders.ALLOW));
    }

    @Test
    void unErrorInesperadoResponde500SinDetalleInterno() throws Exception {
        mockMvc.perform(get(RUTA + "/bug"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado."))
                .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL));
    }
}
