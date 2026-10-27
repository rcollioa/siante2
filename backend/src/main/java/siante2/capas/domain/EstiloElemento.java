package siante2.capas.domain;

/** Estilo que SIATE guarda por elemento (colores CSS, grosor en píxeles e ícono como URL). Vacío o 0 en lo que no se
 * definió: ahí se usa el estilo de la aplicación. */
public record EstiloElemento(String color, double grosor, String relleno, String icono) {

    public static final EstiloElemento SIN_ESTILO = new EstiloElemento("", 0, "", "");
}
