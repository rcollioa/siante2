package siante2.capas.domain;

/** Pestañas del catálogo de capas de SIATE, de donde se eligen favoritas y capas formales. */
public enum SeccionCatalogo {
    /** "Todas": las capas públicas de cualquier usuario. */
    PUBLICAS,
    /** "Usuario": las capas propias. */
    MIAS,
    /** "Capas formales": las de servicios públicos, que SIATE guarda como capas de su usuario de sistema. */
    FORMALES
}
