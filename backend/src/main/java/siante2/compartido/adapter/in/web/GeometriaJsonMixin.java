package siante2.compartido.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.JacksonMixin;
import siante2.compartido.domain.Geometria;

/** Forma JSON de {@link Geometria} sin anotar el dominio: omite {@code coordinates} o {@code geometries}
 * cuando no aplican, como pide GeoJSON. */
@JacksonMixin(Geometria.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class GeometriaJsonMixin {
}
