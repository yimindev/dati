package com.dati.mcp.domain.service;

import com.dati.common.DateTimeUtils;
import com.dati.common.template.CompiledTemplate;
import com.dati.common.template.ParamBinding;
import com.dati.common.template.PreparedSql;
import com.dati.common.template.SqlRenderer;
import com.dati.common.template.TemplateParser;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.analysis.SqlAnalysisResult;
import com.dati.db.analysis.SqlAnalyzer;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig.ParamSqlConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.ToolParameter;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.StatementResult;
import com.dati.mcp.server.pojo.ToolTestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ParameterizedSqlExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final DataSourceService dataSourceService;
    private final TemplateParser templateParser;
    private final SqlRenderer sqlRenderer;
    private final SystemVariableResolver systemVariableResolver;

    public ParameterizedSqlExecutor(ScopeValidator scopeValidator, DataSourceService dataSourceService,
                                    TemplateParser templateParser, SqlRenderer sqlRenderer,
                                    SystemVariableResolver systemVariableResolver) {
        this.scopeValidator = scopeValidator;
        this.dataSourceService = dataSourceService;
        this.templateParser = templateParser;
        this.sqlRenderer = sqlRenderer;
        this.systemVariableResolver = systemVariableResolver;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.PARAMETERIZED_SQL;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        ParamSqlConfig config = (ParamSqlConfig) ctx.config();
        String dsId = config.getDataSourceId();

        CompiledTemplate compiled = templateParser.parse(config.getSqlTemplate());
        Map<String, Object> params = resolveParameters(config, ctx.argumentsMap());
        PreparedSql prepared = sqlRenderer.render(compiled, params);
        String sql = prepared.sql();

        // No runtime SQL policy for PARAMETERIZED_SQL: the template is authored at
        // config time, so operation-type permissions would be self-restriction.
        SqlAnalysisResult analysis = SqlAnalyzer.analyze(sql);

        DataSource dataSource = dataSourceService.getDataSourceInternal(dsId)
            .orElseThrow(() -> new ToolExecuteException(ToolError.DATA_SOURCE_NOT_FOUND, dsId));

        scopeValidator.validate(ctx.scopeItems(), dsId,
            analysis.referencedTables(), analysis.createdTables(),
            dataSource.getDefaultSchema());

        JdbcConnector connector = new JdbcConnector(dataSource);
        List<StatementResult> results;
        try (Connection conn = HikariPoolManager.getConnection(connector);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setQueryTimeout(config.getTimeout());
            stmt.setMaxRows(config.getMaxRows());
            List<ParamBinding> bindings = prepared.bindings();
            var typeMap = config.getParameters().stream()
                .collect(Collectors.toMap(ToolParameter::getName, ToolParameter::getType));
            for (int i = 0; i < bindings.size(); i++) {
                var b = bindings.get(i);
                Object value = b.value();
                if ("DateTime".equals(typeMap.get(b.name())) && value instanceof String s) {
                    try {
                        value = DateTimeUtils.parseDateTime(s);
                    } catch (Exception e) {
                        log.warn("Failed to parse DateTime '{}' for parameter '{}', passing raw value", s, b.name());
                    }
                }
                stmt.setObject(i + 1, value);
            }
            stmt.execute();
            results = SqlExecutorHelper.collect(stmt);
        } catch (SQLException e) {
            throw new ToolExecuteException(ToolError.SQL_EXECUTION_ERROR, e.getMessage());
        }

        return new SqlExecution(sql, results, prepared.bindings().stream()
            .map(ParamBinding::value).toList());
    }

    /**
     * Validates required business parameters and fills configured default values.
     * System variables are resolved server-side and always win over client arguments,
     * so they are merged last and never required from the caller.
     */
    private Map<String, Object> resolveParameters(ParamSqlConfig config, Map<String, Object> arguments) {
        Map<String, Object> params = new HashMap<>(arguments);
        for (ToolParameter parameter : config.getParameters()) {
            String name = parameter.getName();
            if (name == null || SystemVariableResolver.isSystemVariable(name)) continue;
            if (params.get(name) != null) continue;
            if (parameter.isRequired()) {
                throw new ToolExecuteException(ToolError.PARAM_MISSING, name);
            }
            if (parameter.getDefaultValue() != null) {
                params.put(name, toTypedDefault(parameter));
            }
        }
        params.putAll(systemVariableResolver.resolve());
        return params;
    }

    private Object toTypedDefault(ToolParameter parameter) {
        String raw = parameter.getDefaultValue();
        return switch (parameter.getType() == null ? "" : parameter.getType()) {
            case "Number" -> toNumber(parameter.getName(), raw);
            case "Boolean" -> Boolean.parseBoolean(raw);
            case "Array" -> Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
            default -> raw; // String / DateTime (DateTime is converted before JDBC binding)
        };
    }

    private Object toNumber(String name, String raw) {
        String trimmed = raw.trim();
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException ignored) {
            try {
                return new BigDecimal(trimmed);
            } catch (NumberFormatException e) {
                throw new ToolExecuteException(ToolError.PARAM_INVALID,
                    "Invalid default value for parameter '" + name + "': " + raw);
            }
        }
    }
}
