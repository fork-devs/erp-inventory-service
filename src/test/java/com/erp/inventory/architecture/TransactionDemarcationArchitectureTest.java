package com.erp.inventory.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Automated architectural test for transaction demarcation boundaries.
 *
 * <p>Verifies two core invariants:
 *
 * <ol>
 *   <li>No persistence adapter declares its own writable transaction demarcation.
 *       Only {@code @Transactional(readOnly = true)} is permitted for query methods.</li>
 *   <li>No application use case writes to a repository port without coordinating through
 *       the {@code UnitOfWork} port.</li>
 * </ol>
 *
 * <p>Validates source code directly rather than the Spring context, enabling fast execution
 * without booting an application context.
 */
class TransactionDemarcationArchitectureTest {

    private static final Path ADAPTERS = Paths.get(
            "src/main/java/com/erp/inventory/infrastructure/persistence/adapter");
    private static final Path APLICACION = Paths.get(
            "src/main/java/com/erp/inventory/application");

    /** Matches any @Transactional annotation declaration. */
    private static final Pattern TRANSACCIONAL = Pattern.compile("@Transactional\\b");

    /** Matches read-only @Transactional declarations, which are permitted. */
    private static final Pattern SOLO_LECTURA = Pattern.compile(
            "@Transactional\\s*\\(\\s*readOnly\\s*=\\s*true\\s*\\)");

    /** Matches mutating writes invoked on repository ports. */
    private static final Pattern ESCRITURA = Pattern.compile(
            "\\b(\\w+RepositoryPort)\\s*\\.\\s*(save|deleteById|revoke\\w*|invalidate\\w*|update\\w*|mark\\w*)\\s*\\(");

    @Test
    @DisplayName("Ningun adaptador de persistencia declara demarcacion propia")
    void ningunAdaptadorDeclaraSuPropiaDemarcacion() throws IOException {
        List<String> infracciones = new ArrayList<>();

        for (Path fichero : ficherosJava(ADAPTERS)) {
            List<String> lineas = Files.readAllLines(fichero, StandardCharsets.UTF_8);
            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas.get(i);
                if (!TRANSACCIONAL.matcher(linea).find()) {
                    continue;
                }
                if (SOLO_LECTURA.matcher(linea).find()) {
                    continue;
                }
                infracciones.add(nombre(fichero) + ":" + (i + 1) + "  " + linea.trim());
            }
        }

        if (!infracciones.isEmpty()) {
            fail("Los adaptadores de persistencia no deben declarar demarcacion propia: "
                    + "se unen a la transaccion que abre la capa de aplicacion.\n"
                    + "Un @Transactional en un adaptador hace que cada escritura se confirme por "
                    + "separado, y una operacion que necesita ser atomica deja de serlo.\n"
                    + String.join("\n", infracciones));
        }
    }

    @Test
    @DisplayName("Ningun caso de uso escribe sin pasar por el limite transaccional")
    void ningunCasoDeUsoEscribeFueraDelLimite() throws IOException {
        List<String> infracciones = new ArrayList<>();

        for (Path fichero : ficherosJava(APLICACION)) {
            String contenido = Files.readString(fichero, StandardCharsets.UTF_8);

            Set<String> puertosEscritos = new LinkedHashSet<>();
            Matcher matcher = ESCRITURA.matcher(contenido);
            while (matcher.find()) {
                puertosEscritos.add(matcher.group(1));
            }

            if (puertosEscritos.isEmpty()) {
                continue;
            }
            if (contenido.contains("unitOfWork")) {
                continue;
            }

            infracciones.add(nombre(fichero) + "  escribe en " + String.join(", ", puertosEscritos)
                    + " sin usar el limite transaccional");
        }

        if (!infracciones.isEmpty()) {
            fail("Todo caso de uso que escriba debe usar el puerto UnitOfWork: "
                    + "los adaptadores se unen a la transaccion en curso y no abren una propia.\n"
                    + String.join("\n", infracciones));
        }
    }

    /**
     * Application layer must not depend on infrastructure adapters.
     *
     * <p>Core architectural invariant: use cases depend exclusively on DOMAIN ports,
     * maintaining decoupling from concrete infrastructure implementations.
     */
    @Test
    @DisplayName("Ningun caso de uso importa la infraestructura")
    void ningunCasoDeUsoImportaLaInfraestructura() throws IOException {
        List<String> infracciones = new ArrayList<>();

        for (Path fichero : ficherosJava(APLICACION)) {
            List<String> lineas = Files.readAllLines(fichero, StandardCharsets.UTF_8);
            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas.get(i).trim();
                if (linea.startsWith("import com.erp.fleet.infrastructure")) {
                    infracciones.add(nombre(fichero) + ":" + (i + 1) + "  " + linea);
                }
            }
        }

        if (!infracciones.isEmpty()) {
            fail("La capa de aplicacion debe depender de los puertos del dominio, no de los "
                    + "adaptadores: los casos de uso no saben COMO se hace, solo QUE se hace.\n"
                    + String.join("\n", infracciones));
        }
    }

    private List<Path> ficherosJava(Path directorio) throws IOException {
        if (!Files.isDirectory(directorio)) {
            fail("No se encuentra el directorio " + directorio.toAbsolutePath()
                    + ". El test debe ejecutarse desde la raiz del modulo.");
        }
        try (Stream<Path> ficheros = Files.walk(directorio)) {
            return ficheros
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private String nombre(Path fichero) {
        return directorioRaiz().relativize(fichero.toAbsolutePath()).toString();
    }

    private Path directorioRaiz() {
        return Paths.get("").toAbsolutePath();
    }
}
