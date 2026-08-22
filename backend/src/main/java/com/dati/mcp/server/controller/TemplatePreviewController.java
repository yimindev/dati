package com.dati.mcp.server.controller;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.ParamBinding;
import com.dati.common.template.PreparedSql;
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
                PreparedSql ps = sqlRenderer.render(compiled, values);
                response.setRendered(toDisplaySql(ps));
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
     * Convert a PreparedSql to human-readable SQL by inlining bound values.
     * Strings are quoted, numeric strings are left bare, nulls become NULL.
     * This SQL is for DISPLAY ONLY — it is never executed.
     */
    private String toDisplaySql(PreparedSql ps) {
        String sql = ps.sql();
        for (ParamBinding binding : ps.bindings()) {
            sql = sql.replaceFirst("\\?", formatValue(binding.value()));
        }
        return sql;
    }

    private String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof String s) {
            if (s.matches("-?\\d+(\\.\\d+)?")) return s;
            return "'" + s + "'";
        }
        return value.toString();
    }
}
