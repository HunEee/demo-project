package com.example.authapp.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ControllerDependencyArchitectureTest {

    private static final Path API_ROOT = Path.of("src/main/java/com/example/authapp/api");

    @Test
    void controllers_do_not_import_domain_services_or_repositories() throws IOException {
        List<String> violations;
        try (var files = Files.walk(API_ROOT)) {
            violations = files
                    .filter(path -> path.toString().endsWith("Controller.java"))
                    .flatMap(path -> importsIn(path).stream()
                            .filter(ControllerDependencyArchitectureTest::isForbiddenDomainDependency)
                            .map(importLine -> API_ROOT.relativize(path) + " -> " + importLine))
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    private static List<String> importsIn(Path path) {
        try {
            return Files.readAllLines(path)
                    .stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static boolean isForbiddenDomainDependency(String importLine) {
        return importLine.matches("import com\\.example\\.authapp\\.domain\\..*\\.(service|repository)\\..*;")
                || importLine.equals("import com.example.authapp.domain.admin.AdminConsoleService;")
                || importLine.equals("import com.example.authapp.domain.admin.AdminService;")
                || importLine.equals("import com.example.authapp.domain.admin.AdminSettingsStore;");
    }
}
