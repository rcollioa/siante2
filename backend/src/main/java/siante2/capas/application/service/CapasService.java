package siante2.capas.application.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;

/** El dueño sale de la identidad de WebSEAL, nunca de la solicitud (SIATE lo tomaba de la URL). */
@Service
public class CapasService implements ListarMisCapasUseCase, VerCapaUseCase, ListarFavoritasUseCase, VerCatalogoUseCase {

    private static final Locale ESPANOL = Locale.of("es", "CL");
    /** ID que ningún usuario tiene: sin registro en USERS no hay capas propias ni favoritas. */
    private static final int SIN_USUARIO = -1;

    private final CapasReadPort capas;
    private final IdentificarUsuarioUseCase usuarios;

    public CapasService(CapasReadPort capas, IdentificarUsuarioUseCase usuarios) {
        this.capas = capas;
        this.usuarios = usuarios;
    }

    @Override
    public List<ResumenCapa> listar(String ivUser) {
        return porNombre(usuarios.idDe(ivUser).map(capas::listarDe).orElse(List.of()), ResumenCapa::nombre);
    }

    @Override
    public List<ResumenCapa> listarFavoritas(String ivUser) {
        return porNombre(usuarios.idDe(ivUser).map(capas::listarFavoritasDe).orElse(List.of()), ResumenCapa::nombre);
    }

    /** Un usuario que nunca ingresó ve igual las capas públicas y formales, sin ninguna favorita ni propia. */
    @Override
    public List<CapaCatalogo> verCatalogo(SeccionCatalogo seccion, String ivUser) {
        int usuario = usuarios.idDe(ivUser).orElse(SIN_USUARIO);
        return porNombre(capas.catalogo(seccion, usuario), CapaCatalogo::nombre);
    }

    /** Por nombre como en un diccionario: "Árboles" junto a "Arroyos" y no después de la Z, como en la base. */
    private static <T> List<T> porNombre(List<T> lista, Function<T, String> nombre) {
        return lista.stream().sorted(Comparator.comparing(nombre, Collator.getInstance(ESPANOL))).toList();
    }

    @Override
    public Capa ver(int capaId, String ivUser) {
        return usuarios.idDe(ivUser)
                .flatMap(usuario -> capas.buscarVisible(capaId, usuario))
                .orElseThrow(() -> new RecursoNoEncontradoException("La capa no existe."));
    }
}
