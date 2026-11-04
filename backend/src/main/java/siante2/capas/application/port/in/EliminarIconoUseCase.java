package siante2.capas.application.port.in;

public interface EliminarIconoUseCase {
    /** Como SIATE, lo deja inactivo sin borrarlo; los elementos que ya lo usan conservan su copia. Lanza
     * {@code RecursoNoEncontradoException} si no existe o no es del usuario. */
    void eliminar(int id, String ivUser);
}
