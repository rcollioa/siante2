package siante2.multimedia.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.multimedia.application.port.in.ListarMultimediaUseCase;
import siante2.multimedia.application.port.in.ObtenerArchivoMultimediaUseCase;
import siante2.multimedia.domain.ArchivoMultimedia;
import siante2.multimedia.domain.Multimedia;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MultimediaController.class)
@Import(UsuarioDePrueba.class)
class MultimediaControllerTest {

    private static final String RUTA = "/siante2/api/multimedia";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarMultimediaUseCase listar;

    @MockitoBean
    private ObtenerArchivoMultimediaUseCase obtener;

    @Test
    void listaSinContenido() throws Exception {
        when(listar.listar()).thenReturn(List.of(new Multimedia(3, "Manual SIATE")));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].descripcion").value("Manual SIATE"));
    }

    @Test
    void elManualSeDescargaConSuNombreYUnGifSeMuestraEnLinea() throws Exception {
        byte[] pdf = "%PDF-1.7".getBytes(StandardCharsets.ISO_8859_1);
        when(obtener.obtener(3)).thenReturn(ArchivoMultimedia.desde("Manual SIATE", pdf));
        when(obtener.obtener(4)).thenReturn(ArchivoMultimedia.desde("Gif Metadata", "GIF89a".getBytes(StandardCharsets.ISO_8859_1)));

        mockMvc.perform(get(RUTA + "/3/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("Manual_SIATE.pdf")))
                .andExpect(content().bytes(pdf));
        mockMvc.perform(get(RUTA + "/4/archivo"))
                .andExpect(content().contentType("image/gif"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")));
    }

    @Test
    void unArchivoInexistenteResponde404ConElFormatoDeErrores() throws Exception {
        when(obtener.obtener(9)).thenThrow(new RecursoNoEncontradoException("No existe el archivo multimedia 9."));

        mockMvc.perform(get(RUTA + "/9/archivo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No existe el archivo multimedia 9."));
    }
}
