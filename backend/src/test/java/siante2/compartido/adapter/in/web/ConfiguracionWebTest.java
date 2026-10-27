package siante2.compartido.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prefijo de la API ({@link ApiPrefijoConfig}), caché ({@link CacheControlAdvice}, {@link EtagApiFilter}) y
 * errores ({@link ApiExceptionHandler}), sobre {@link ControladorPrueba} y {@link CatalogoPrueba}. */
@WebMvcTest({ControladorPrueba.class, CatalogoPrueba.class})
@Import(UsuarioDePrueba.class)
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
    void unGetExitosoSeRevalidaSiempreYNoQuedaEnCachesCompartidas() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, private"))
                .andExpect(header().exists(HttpHeaders.ETAG));
    }

    @Test
    void conElMismoEtagResponde304SinCuerpo() throws Exception {
        String etag = mockMvc.perform(get(RUTA)).andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get(RUTA).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    void unCatalogoSeGuardaEnElNavegadorPeroNoEnCachesCompartidas() throws Exception {
        mockMvc.perform(get(RUTA + "/catalogo"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, private"));
    }

    @Test
    void unArchivoNoLlevaEtag() throws Exception {
        mockMvc.perform(get(RUTA + "/catalogo/archivo"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ETAG));
    }

    @Test
    void unPostNoSeCachea() throws Exception {
        mockMvc.perform(post(RUTA))
                .andExpect(status().isOk())
                .andExpect(noSeCachea());
    }

    @Test
    void unaSolicitudInvalidaResponde400SinCachear() throws Exception {
        mockMvc.perform(get(RUTA + "/invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Año fuera de rango"))
                .andExpect(noSeCachea());
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
                .andExpect(noSeCachea());
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
                .andExpect(noSeCachea());
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
                .andExpect(noSeCachea());
    }

    @Test
    void unaFuncionProtegidaPorGrupoAtiendeAQuienTieneElGrupo() throws Exception {
        mockMvc.perform(get(RUTA + "/grupo-prueba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("permitido"));
    }

    @Test
    void unaFuncionDeOtroGrupoResponde403ConElMismoFormato() throws Exception {
        mockMvc.perform(get(RUTA + "/grupo-otro"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("No tiene permiso para esta operación."));
    }

    /** Sin Cache-Control o con no-store (Spring Security lo agrega a las respuestas que no cachea CacheControlAdvice). */
    private static ResultMatcher noSeCachea() {
        return resultado -> {
            String valor = resultado.getResponse().getHeader(HttpHeaders.CACHE_CONTROL);
            assertThat(valor == null || valor.contains("no-store")).as("Cache-Control: %s", valor).isTrue();
        };
    }
}
