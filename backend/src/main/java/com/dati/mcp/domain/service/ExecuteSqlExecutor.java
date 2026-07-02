package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.analysis.SqlAnalysisResult;
import com.dati.db.analysis.SqlAnalyzer;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.SqlPolicy;
import com.dati.mcp.domain.model.ToolConfig.ExecuteSqlConfig;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.StatementResult;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Component
public class ExecuteSqlExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final DataSourceService dataSourceService;

    public ExecuteSqlExecutor(ScopeValidator scopeValidator, DataSourceService dataSourceService) {
        this.scopeValidator = scopeValidator;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.EXECUTE_SQL;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        String dsId = (String) ctx.arguments().get("data_source_id");
        String sql = (String) ctx.arguments().get("sql");
        if (dsId == null || dsId.isBlank()) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "data_source_id is required");
        }
        if (sql == null || sql.isBlank()) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "sql is required");
        }

        ExecuteSqlConfig config = (ExecuteSqlConfig) ctx.config();
        if (config == null) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "tool config is missing");
        }

        SqlPolicy policy = config.getSqlPolicy();
        if (policy == null) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "SQL policy is not configured");
        }

        SqlAnalysisResult analysis = SqlAnalyzer.analyze(sql);

        policy.validate(analysis);

        DataSource dataSource = dataSourceService.getDataSource(dsId);

        scopeValidator.validate(ctx.scopeItems(), dsId, analysis.tables(),
            dataSource.getDefaultSchema());

        JdbcConnector connector = new JdbcConnector(dataSource);
        List<StatementResult> results;
        try (Connection conn = HikariPoolManager.getConnection(connector);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(config.getTimeout());
            stmt.setMaxRows(config.getMaxRows());
            stmt.execute(sql);
            results = SqlExecutorHelper.collect(stmt);
        } catch (SQLException e) {
            throw new DatiException(ErrorCode.DS_SQL_ERROR, e.getMessage());
        }

        return new SqlExecution(sql, results, null);
    }
}
