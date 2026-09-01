package com.dati.mcp.domain.service;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.ToolError;
import jakarta.annotation.PreDestroy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deserializes tool arguments into the parameter record and validates them.
 * All failures map to ToolExecuteException(PARAM_INVALID); dynamic tools
 * (PARAMETERIZED_SQL) have no record contract and pass through (returns null).
 * The validator is pinned to English so protocol-level error messages are
 * deterministic for LLM clients regardless of the server locale.
 */
@Component
public class ToolParameterBinder {

    private final ValidatorFactory validatorFactory;
    private final Validator validator;

    public ToolParameterBinder() {
        this.validatorFactory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ResourceBundleMessageInterpolator(
                Set.of(Locale.ENGLISH), Locale.ENGLISH,
                context -> Locale.ENGLISH, false))
            .buildValidatorFactory();
        this.validator = validatorFactory.getValidator();
    }

    /** Closes the validator factory on application shutdown (Spring-managed lifecycle). */
    @PreDestroy
    public void close() {
        validatorFactory.close();
    }

    /**
     * Converts raw arguments into the tool's expected shape: parameter record for
     * prebuilt tools, raw map for dynamic tools (PARAMETERIZED_SQL, no contract).
     * Prebuilt failures map to ToolExecuteException(PARAM_INVALID).
     */
    public Object bind(Class<?> type, Map<String, Object> arguments) {
        if (type == null) {
            return arguments == null ? Map.of() : arguments;
        }
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        Object record;
        try {
            record = JsonUtils.fromJson(JsonUtils.toJson(args), type);
        } catch (RuntimeException e) {
            throw new ToolExecuteException(ToolError.PARAM_INVALID, e.getMessage());
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(record);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
            throw new ToolExecuteException(ToolError.PARAM_INVALID, message);
        }
        return record;
    }
}
