package com.example.authapp.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.authapp.api.AdminController;
import com.example.authapp.api.MfaController;

class AdminMfaControllerContractTest {

    @Test
    void pathVariablesDeclareNamesBecauseParameterReflectionIsNotEnabled() {
        assertPathVariablesNamed(AdminController.class);
        assertPathVariablesNamed(AdminApiPermissionRuleController.class);
        assertPathVariablesNamed(AdminPermissionController.class);
        assertPathVariablesNamed(AdminMfaController.class);
        assertPathVariablesNamed(AdminRiskController.class);
        assertPathVariablesNamed(AdminSecurityController.class);
        assertPathVariablesNamed(AdminSessionController.class);
        assertPathVariablesNamed(MfaController.class);
    }

    private void assertPathVariablesNamed(Class<?> controllerType) {
        for (Method method : controllerType.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                if (pathVariable != null) {
                    assertThat(pathVariable.value().isBlank() ? pathVariable.name() : pathVariable.value())
                            .as(controllerType.getSimpleName() + "." + method.getName())
                            .isNotBlank();
                }
            }
        }
    }
}
