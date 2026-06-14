package com.example.authapp.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class DomainServiceDependencyArchitectureTest {

    private static final Path DOMAIN_ROOT = Path.of("src/main/java/com/example/authapp/domain");
    private static final Pattern DOMAIN_PACKAGE_PATTERN = Pattern.compile("package com\\.example\\.authapp\\.domain\\.([^.]+).*;");
    private static final Pattern DOMAIN_SERVICE_OR_REPOSITORY_IMPORT_PATTERN =
            Pattern.compile("import com\\.example\\.authapp\\.domain\\.([^.]+)\\.(?:.*\\.)?(service|repository)\\..*;");

    @Test
    void domain_services_do_not_import_web_request_response_or_cookie_types() throws IOException {
        List<String> violations;
        try (var files = Files.walk(DOMAIN_ROOT)) {
            violations = files
                    .filter(DomainServiceDependencyArchitectureTest::isDomainServiceFile)
                    .flatMap(path -> importsIn(path).stream()
                            .filter(DomainServiceDependencyArchitectureTest::isForbiddenWebDependency)
                            .map(importLine -> DOMAIN_ROOT.relativize(path) + " -> " + importLine))
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void domain_services_do_not_import_other_domain_services_or_repositories() throws IOException {
        List<String> violations;
        try (var files = Files.walk(DOMAIN_ROOT)) {
            violations = files
                    .filter(DomainServiceDependencyArchitectureTest::isDomainServiceFile)
                    .flatMap(path -> importsIn(path).stream()
                            .filter(importLine -> isOtherDomainServiceOrRepositoryImport(path, importLine))
                            .map(importLine -> DOMAIN_ROOT.relativize(path) + " -> " + importLine))
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    private static boolean isDomainServiceFile(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.endsWith("Service.java")
                && (normalized.contains("/service/") || normalized.matches(".*/domain/admin/.*Service\\.java"));
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

    private static boolean isForbiddenWebDependency(String importLine) {
        return importLine.equals("import jakarta.servlet.http.HttpServletRequest;")
                || importLine.equals("import jakarta.servlet.http.HttpServletResponse;")
                || importLine.equals("import jakarta.servlet.http.Cookie;");
    }

    private static boolean isOtherDomainServiceOrRepositoryImport(Path path, String importLine) {
        var importMatcher = DOMAIN_SERVICE_OR_REPOSITORY_IMPORT_PATTERN.matcher(importLine);
        if (!importMatcher.matches()) {
            return false;
        }

        String ownDomain = domainNameOf(path);
        String importedDomain = importMatcher.group(1);
        return ownDomain != null && !ownDomain.equals(importedDomain);
    }

    private static String domainNameOf(Path path) {
        try {
            return Files.readAllLines(path)
                    .stream()
                    .map(DOMAIN_PACKAGE_PATTERN::matcher)
                    .filter(matcher -> matcher.matches())
                    .map(matcher -> matcher.group(1))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
