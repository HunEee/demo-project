package com.example.authapp.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;

class AdminUserControllerContractTest {

    @Test
    void exposesCanonicalUserManagementEndpoints() {
        assertThat(patchMappings()).contains(
                "/{username}",
                "/{username}/status",
                "/{username}/lock",
                "/{username}/unlock"
        );
        assertThat(postMappings()).contains(
                "",
                "/{username}/reset-password",
                "/{username}/revoke-tokens",
                "/{username}/reset-mfa"
        );
        assertThat(getMappings()).contains("", "/{username}");
    }

    @Test
    void removesLegacyActionEndpoints() {
        Set<String> allMappings = allMappings();

        assertThat(allMappings).doesNotContain(
                "POST /{username}/delete",
                "POST /{username}/disable",
                "POST /{username}/enable",
                "POST /{username}/password/reset",
                "POST /{username}/tokens/revoke",
                "POST /{username}/mfa/reset"
        );
    }

    private Set<String> getMappings() {
        return Arrays.stream(AdminUserController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(GetMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> mappingValues(annotation.value()))
                .collect(Collectors.toSet());
    }

    private Set<String> postMappings() {
        return Arrays.stream(AdminUserController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> mappingValues(annotation.value()))
                .collect(Collectors.toSet());
    }

    private Set<String> patchMappings() {
        return Arrays.stream(AdminUserController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PatchMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> mappingValues(annotation.value()))
                .collect(Collectors.toSet());
    }

    private Set<String> allMappings() {
        return Arrays.stream(AdminUserController.class.getDeclaredMethods())
                .flatMap(this::methodMappings)
                .collect(Collectors.toSet());
    }

    private java.util.stream.Stream<String> methodMappings(Method method) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        PostMapping post = method.getAnnotation(PostMapping.class);
        PatchMapping patch = method.getAnnotation(PatchMapping.class);
        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
        if (get != null) return mappingValues(get.value()).map(value -> "GET " + value);
        if (post != null) return mappingValues(post.value()).map(value -> "POST " + value);
        if (patch != null) return mappingValues(patch.value()).map(value -> "PATCH " + value);
        if (delete != null) return mappingValues(delete.value()).map(value -> "DELETE " + value);
        return java.util.stream.Stream.empty();
    }

    private java.util.stream.Stream<String> mappingValues(String[] values) {
        if (values.length == 0) return java.util.stream.Stream.of("");
        return Arrays.stream(values);
    }
}
