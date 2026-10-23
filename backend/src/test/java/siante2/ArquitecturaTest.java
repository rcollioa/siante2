package siante2;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Reglas de la arquitectura hexagonal por módulo (ver README). Si una falla, se corrige el código,
 * no la regla; una excepción nueva se discute y se documenta aquí. */
@AnalyzeClasses(packages = "siante2", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String COMPARTIDO = "compartido";

    @ArchTest
    static final ArchRule dominioSinFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..",
                    "tools.jackson..", "com.fasterxml.jackson..")
            .because("el dominio es Java puro: no depende de la web ni de la base");

    @ArchTest
    static final ArchRule casosDeUsoSinAdaptadores = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("los casos de uso hablan con el exterior solo por puertos");

    @ArchTest
    static final ArchRule controladoresSinAccesoADatos = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("un controlador llama a un caso de uso, nunca a un repositorio");

    @ArchTest
    static final ArchRule entidadesSoloEnSuAdaptador = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..adapter.out.oracle..")
            .andShould().onlyHaveDependentClassesThat().resideInAPackage("..adapter.out.oracle..")
            .because("las entidades JPA no salen del adaptador: ni al dominio ni a la respuesta JSON");

    @ArchTest
    static final ArchRule repositoriosNoPublicos = classes()
            .that().areAssignableTo(Repository.class)
            .should().resideInAPackage("..adapter.out.oracle..")
            .andShould().notBePublic()
            .because("solo su adaptador usa el repositorio de Spring Data");

    @ArchTest
    static final ArchRule controladoresEnSuPaquete = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..adapter.in.web..")
            .andShould().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule modulosSinAdaptadoresAjenos = noClasses()
            .should(usarAdaptadoresDeOtroModulo())
            .because("un módulo usa de otro solo su dominio y sus puertos; compartido lo usan todos");

    @ArchTest
    static final ArchRule compartidoNoDependeDeModulos = noClasses()
            .that().resideInAPackage("siante2.compartido..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "siante2.categorias..", "siante2.usuarios..", "siante2.capas..", "siante2.favoritos..",
                    "siante2.grupos..", "siante2.reportes..", "siante2.multimedia..", "siante2.direcciones..")
            .because("compartido es la base de todos los módulos y no conoce a ninguno");

    @ArchTest
    static final ArchRule modulosSinCiclos = slices().matching("siante2.(*)..").should().beFreeOfCycles();

    /** Módulo de una clase: el paquete que sigue a {@code siante2}, o vacío para BackendApplication. */
    private static String modulo(JavaClass clase) {
        String[] partes = clase.getPackageName().split("\\.");
        return partes.length > 1 ? partes[1] : "";
    }

    private static ArchCondition<JavaClass> usarAdaptadoresDeOtroModulo() {
        return new ArchCondition<>("depend on adapters of another module") {
            @Override
            public void check(JavaClass origen, ConditionEvents eventos) {
                String propio = modulo(origen);
                origen.getDirectDependenciesFromSelf().stream()
                        .map(d -> d.getTargetClass())
                        .filter(destino -> destino.getPackageName().startsWith("siante2.")
                                && destino.getPackageName().contains(".adapter.")
                                && !modulo(destino).equals(propio)
                                && !modulo(destino).equals(COMPARTIDO))
                        .forEach(destino -> eventos.add(SimpleConditionEvent.satisfied(origen,
                                origen.getName() + " usa " + destino.getName())));
            }
        };
    }
}
