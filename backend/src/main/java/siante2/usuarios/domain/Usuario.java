package siante2.usuarios.domain;

import java.util.List;

/** Usuario de la tabla {@code USERS} con los grupos de WebSEAL de la solicitud. Textos vacíos, nunca null. */
public record Usuario(int id, String ivUser, String nombre, String apellido, String email, List<String> grupos) {
}
