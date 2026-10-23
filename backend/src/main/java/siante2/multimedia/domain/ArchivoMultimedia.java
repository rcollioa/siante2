package siante2.multimedia.domain;

/** Contenido de un archivo multimedia listo para entregar, con su nombre y tipo detectados. */
public record ArchivoMultimedia(String nombre, String tipoContenido, byte[] contenido) {

    private static final String PDF = "application/pdf";
    private static final String GIF = "image/gif";
    private static final String PNG = "image/png";

    /** El tipo sale de los primeros bytes (SIANTE no lo guarda); el nombre, de la descripción. */
    public static ArchivoMultimedia desde(String descripcion, byte[] contenido) {
        String tipo;
        String extension;
        if (empiezaCon(contenido, 0x25, 0x50, 0x44, 0x46)) {
            tipo = PDF;
            extension = ".pdf";
        } else if (empiezaCon(contenido, 0x47, 0x49, 0x46, 0x38)) {
            tipo = GIF;
            extension = ".gif";
        } else if (empiezaCon(contenido, 0x89, 0x50, 0x4E, 0x47)) {
            tipo = PNG;
            extension = ".png";
        } else {
            tipo = "application/octet-stream";
            extension = "";
        }
        String base = descripcion == null || descripcion.isBlank() ? "archivo" : descripcion.trim().replaceAll("\\s+", "_");
        return new ArchivoMultimedia(base + extension, tipo, contenido);
    }

    public boolean esImagen() {
        return tipoContenido.startsWith("image/");
    }

    private static boolean empiezaCon(byte[] contenido, int... firma) {
        if (contenido == null || contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((contenido[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
