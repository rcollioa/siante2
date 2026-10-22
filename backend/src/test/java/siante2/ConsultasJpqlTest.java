package siante2;

import jakarta.persistence.Entity;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;

/** Compila cada {@code @Query} JPQL de los repositorios contra el modelo de entidades, sin Oracle.
 * Las consultas nativas quedan fuera. */
class ConsultasJpqlTest {

    private static final String PAQUETE_ENTIDADES = "siante2";
    private static final String PAQUETE_REPOSITORIOS = "siante2";

    private static StandardServiceRegistry registro;
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void construirModelo() {
        registro = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();
        MetadataSources fuentes = new MetadataSources(registro);
        clases(PAQUETE_ENTIDADES, new AnnotationTypeFilter(Entity.class)).forEach(fuentes::addAnnotatedClass);
        sessionFactory = fuentes.buildMetadata().buildSessionFactory();
    }

    @AfterAll
    static void cerrar() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        StandardServiceRegistryBuilder.destroy(registro);
    }

    @TestFactory
    Stream<DynamicTest> cadaConsultaJpqlCompilaContraElModelo() {
        return clases(PAQUETE_REPOSITORIOS, new AssignableTypeFilter(Repository.class)).stream()
                .flatMap(repositorio -> Arrays.stream(repositorio.getDeclaredMethods())
                        .sorted(Comparator.comparing(Method::getName))
                        .filter(metodo -> metodo.isAnnotationPresent(Query.class))
                        .filter(metodo -> !metodo.getAnnotation(Query.class).nativeQuery())
                        .map(metodo -> DynamicTest.dynamicTest(
                                repositorio.getSimpleName() + "." + metodo.getName(),
                                () -> assertThatCode(() -> sessionFactory.inSession(sesion ->
                                        sesion.createSelectionQuery(metodo.getAnnotation(Query.class).value(),
                                                tipoDeResultado(metodo))))
                                        .doesNotThrowAnyException())));
    }

    /** {@code List<X>}, {@code Page<X>}, {@code Stream<X>} u {@code Optional<X>} → {@code X}; cualquier otro retorno tal cual. */
    private static Class<?> tipoDeResultado(Method metodo) {
        ResolvableType retorno = ResolvableType.forMethodReturnType(metodo);
        Class<?> contenedor = retorno.toClass();
        if (Iterable.class.isAssignableFrom(contenedor) || Stream.class.isAssignableFrom(contenedor)
                || Optional.class.equals(contenedor)) {
            return retorno.getGeneric(0).toClass();
        }
        return ClassUtils.resolvePrimitiveIfNecessary(retorno.toClass());
    }

    private static List<Class<?>> clases(String paquete, org.springframework.core.type.filter.TypeFilter filtro) {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                    org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definicion) {
                return true; // incluye interfaces (los repositorios)
            }
        };
        escaner.addIncludeFilter(filtro);
        return escaner.findCandidateComponents(paquete).stream()
                .map(BeanDefinition::getBeanClassName)
                .sorted()
                .<Class<?>>map(nombre -> ClassUtils.resolveClassName(nombre, ConsultasJpqlTest.class.getClassLoader()))
                .toList();
    }
}
