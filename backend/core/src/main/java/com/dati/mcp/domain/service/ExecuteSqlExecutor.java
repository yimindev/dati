package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.analysis.SqlAnalysisResult;
import com.dati.db.analysis.SqlAnalyzer;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.SqlPolicy;
import com.dati.mcp.domain.model.ToolConfig.ExecuteSqlConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.StatementResult;
import com.dati.mcp.server.pojo.ToolTestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Component
public class ExecuteSqlExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final DataSourceService dataSourceService;
    private final TableAutoRegistrationService tableAutoRegistrationService;

    public ExecuteSqlExecutor(ScopeValidator scopeValidator,
                              DataSourceService dataSourceService,
                              TableAutoRegistrationService tableAutoRegistrationService) {
        this.scopeValidator = scopeValidator;
        this.dataSourceService = dataSourceService;
        this.tableAutoRegistrationService = tableAutoRegistrationService;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.EXECUTE_SQL;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        ExecuteSqlArgs args = ctx.args(ExecuteSqlArgs.class);
        String dsId = args.dataSourceId();
        String sql = args.sql();

        ExecuteSqlConfig config = (ExecuteSqlConfig) ctx.config();

        SqlPolicy policy = config.getSqlPolicy();

        SqlAnalysisResult analysis = SqlAnalyzer.analyze(sql);

        policy.validate(analysis);

        DataSource dataSource = dataSourceService.getDataSourceInternal(dsId)
            .orElseThrow(() -> new ToolExecuteException(ToolError.DATA_SOURCE_NOT_FOUND, dsId));

        scopeValidator.validate(ctx.scopeItems(), dsId,
            analysis.referencedTables(), analysis.createdTables(),
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
            throw new ToolExecuteException(ToolError.SQL_EXECUTION_ERROR, e.getMessage());
        }

        registerCreatedTables(dsId, analysis, dataSource.getDefaultSchema(), ctx.serviceId());

        return new SqlExecution(sql, results, null);
    }

    private void registerCreatedTables(String dsId, SqlAnalysisResult analysis,
                                       String defaultSchema, String serviceId) {
        if (analysis.createdTables().isEmpty()) {
            return;
        }
        try {
            tableAutoRegistrationService.registerCreatedTables(
                dsId, analysis.createdTables(), defaultSchema, serviceId);
        } catch (Exception e) {
            log.warn("Failed to auto-register tables created by SQL on data source {}: {}",
                dsId, e.getMessage());
        }
    }
}
