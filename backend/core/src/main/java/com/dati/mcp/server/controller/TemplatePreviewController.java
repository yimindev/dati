package com.dati.mcp.server.controller;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.SqlRenderer;
import com.dati.common.template.TemplateParseException;
import com.dati.common.template.TemplateParser;
import com.dati.common.template.TemplateRenderException;
import com.dati.common.template.TextRenderer;
import com.dati.mcp.domain.service.SystemVariableResolver;
import com.dati.mcp.domain.model.TemplateRenderMode;
import com.dati.mcp.server.pojo.TemplateExtractRequest;
import com.dati.mcp.server.pojo.TemplateExtractResponse;
import com.dati.mcp.server.pojo.TemplatePreviewRequest;
import com.dati.mcp.server.pojo.TemplatePreviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/template")
public class TemplatePreviewController {

    private final TemplateParser parser;
    private final TextRenderer textRenderer;
    private final SqlRenderer sqlRenderer;
    private final SystemVariableResolver systemVariableResolver;

    public TemplatePreviewController(TemplateParser parser,
                                      TextRenderer textRenderer,
                                      SqlRenderer sqlRenderer,
                                      SystemVariableResolver systemVariableResolver) {
        this.parser = parser;
        this.textRenderer = textRenderer;
        this.sqlRenderer = sqlRenderer;
        this.systemVariableResolver = systemVariableResolver;
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@Valid @RequestBody TemplatePreviewRequest request) {
        CompiledTemplate compiled;
        try {
            compiled = parser.parse(request.getTemplate());
        } catch (TemplateParseException e) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, e.getMessage());
        }

        Map<String, Object> values = new HashMap<>();
        if (request.getValues() != null) {
            values.putAll(request.getValues());
        }
        values.putAll(systemVariableResolver.resolve());

        TemplatePreviewResponse response = new TemplatePreviewResponse();

        if (request.getMode() == TemplateRenderMode.TEXT) {
            response.setRendered(textRenderer.render(compiled, values));
        } else {
            try {
                response.setRendered(sqlRenderer.renderDisplay(compiled, values, this::formatValue));
            } catch (TemplateRenderException e) {
                throw new DatiException(ErrorCode.INVALID_PARAMETER, e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/extract")
    public ResponseEntity<TemplateExtractResponse> extract(@Valid @RequestBody TemplateExtractRequest request) {
        try {
            CompiledTemplate compiled = parser.parse(request.getTemplate());
            Set<String> variables = compiled.getVariables().stream()
                    .filter(v -> !SystemVariableResolver.isSystemVariable(v))
                    .collect(Collectors.toSet());
            return ResponseEntity.ok(new TemplateExtractResponse(variables));
        } catch (TemplateParseException e) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, e.getMessage());
        }
    }

    /**
     * Formats a bound value as a SQL literal for DISPLAY ONLY (never executed).
     * Numeric strings are left bare; other strings are quoted and single quotes
     * are escaped per the SQL standard.
     */
    private String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof String s) {
            if (s.matches("-?\\d+(\\.\\d+)?")) return s;
            return "'" + s.replace("'", "''") + "'";
        }
        return value.toString();
    }
}
