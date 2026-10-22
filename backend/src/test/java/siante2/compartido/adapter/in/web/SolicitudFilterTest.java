package siante2.compartido.adapter.in.web;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudFilterTest {

    private final SolicitudFilter filtro = new SolicitudFilter();

    private String procesar(MockHttpServletRequest request, MockHttpServletResponse response,
                            AtomicReference<String> enMdc) throws Exception {
        filtro.doFilter(request, response, (req, res) -> enMdc.set(MDC.get(SolicitudFilter.CLAVE_MDC)));
        return response.getHeader(SolicitudFilter.CABECERA);
    }

    @Test
    void sinCabeceraGeneraUnIdYLoDejaEnElMdcDuranteLaSolicitud() throws Exception {
        AtomicReference<String> enMdc = new AtomicReference<>();
        String id = procesar(new MockHttpServletRequest(), new MockHttpServletResponse(), enMdc);

        assertThat(id).isNotBlank();
        assertThat(enMdc.get()).isEqualTo(id);
        assertThat(MDC.get(SolicitudFilter.CLAVE_MDC)).isNull();
    }

    @Test
    void reutilizaElIdValidoQueLlegaDelIngress() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SolicitudFilter.CABECERA, "abc-123");

        assertThat(procesar(request, new MockHttpServletResponse(), new AtomicReference<>())).isEqualTo("abc-123");
    }

    @Test
    void reemplazaUnIdConCaracteresNoPermitidos() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SolicitudFilter.CABECERA, "x\r\ninyectado");

        assertThat(procesar(request, new MockHttpServletResponse(), new AtomicReference<>()))
                .isNotEqualTo("x\r\ninyectado")
                .matches("[0-9a-f-]{36}");
    }
}
