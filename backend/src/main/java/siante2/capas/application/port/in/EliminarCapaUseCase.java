package siante2.capas.application.port.in;

public interface EliminarCapaUseCase {
    /** Lanza {@code RecursoNoEncontradoException} si la capa no existe o no es del usuario, sin distinguir los casos. */
    void eliminar(int capaId, String ivUser);
}
