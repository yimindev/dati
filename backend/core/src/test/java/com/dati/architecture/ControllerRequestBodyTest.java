package com.dati.architecture;

import com.dati.base.pojo.BaseResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the request/domain boundary: controller request bodies are dedicated request DTOs,
 * never domain models or the VOs derived from them.
 */
@DisplayName("Controller request bodies must be dedicated request DTOs")
class ControllerRequestBodyTest {

    @Test
    @DisplayName("no @RequestBody parameter is a BaseResource (domain model or VO)")
    void requestBodiesMustNotBeDomainModels() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Class<?> controller : scanRestControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    if (!parameters[i].isAnnotationPresent(RequestBody.class)) {
                        continue;
                    }
                    Class<?> bodyType = unwrap(ResolvableType.forMethodParameter(
                            method, i, controller));
                    if (bodyType == null || isPlatformType(bodyType)) {
                        continue;
                    }
                    if (BaseResource.class.isAssignableFrom(bodyType)) {
                        violations.add(controller.getSimpleName() + "#" + method.getName()
                                + " -> " + bodyType.getName());
                    }
                }
            }
        }

        assertThat(violations)
                .as("controller request bodies must be *Request DTOs")
                .isEmpty();
    }

    /** Unwraps List<T>/Collection<T> so List<@Valid SomeRequest> is checked by element type. */
    private Class<?> unwrap(ResolvableType type) {
        Class<?> raw = type.resolve();
        if (raw != null && Collection.class.isAssignableFrom(raw)) {
            Class<?> element = type.getGeneric(0).resolve();
            return element != null ? element : raw;
        }
        return raw;
    }

    private boolean isPlatformType(Class<?> type) {
        return type.isPrimitive()
                || type.getName().startsWith("java.")
                || type.getName().startsWith("jakarta.")
                || type.getName().startsWith("com.fasterxml.");
    }

    private List<Class<?>> scanRestControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> controllers = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.dati")) {
            controllers.add(Class.forName(candidate.getBeanClassName()));
        }
        assertThat(controllers).as("controller scan sanity check").isNotEmpty();
        return controllers;
    }
}
