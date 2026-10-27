package siante2.capas.application.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.ResumenCapa;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;

/** El dueño sale de la identidad de WebSEAL, nunca de la solicitud (SIATE lo tomaba de la URL). */
@Service
public class CapasService implements ListarMisCapasUseCase, VerCapaUseCase {

    private static final Locale ESPANOL = Locale.of("es", "CL");

    private final CapasReadPort capas;
    private final IdentificarUsuarioUseCase usuarios;

    public CapasService(CapasReadPort capas, IdentificarUsuarioUseCase usuarios) {
        this.capas = capas;
        this.usuarios = usuarios;
    }

    /** Por nombre como en un diccionario: "Árboles" junto a "Arroyos" y no después de la Z, como en la base. */
    @Override
    public List<ResumenCapa> listar(String ivUser) {
        Collator espanol = Collator.getInstance(ESPANOL);
        return usuarios.idDe(ivUser).map(capas::listarDe).orElse(List.of()).stream()
                .sorted(Comparator.comparing(ResumenCapa::nombre, espanol))
                .toList();
    }

    @Override
    public Capa ver(int capaId, String ivUser) {
        return usuarios.idDe(ivUser)
                .flatMap(usuario -> capas.buscarVisible(capaId, usuario))
                .orElseThrow(() -> new RecursoNoEncontradoException("La capa no existe."));
    }
}
