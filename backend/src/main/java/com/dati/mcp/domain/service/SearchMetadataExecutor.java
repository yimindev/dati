package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolError;
import com.dati.datasource.domain.model.DataSourceDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.mcp.server.pojo.SearchHit;
import com.dati.mcp.server.pojo.ToolTestData;
import com.dati.semantic.domain.model.TermDef;
import com.dati.semantic.domain.service.SemanticSearchService;
import com.dati.semantic.domain.service.SemanticSearchService.SearchResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class SearchMetadataExecutor implements ToolExecutor {

    private final McpServiceDataScopeService dataScopeService;
    private final SemanticSearchService semanticSearchService;

    public SearchMetadataExecutor(McpServiceDataScopeService dataScopeService,
                                   SemanticSearchService semanticSearchService) {
        this.dataScopeService = dataScopeService;
        this.semanticSearchService = semanticSearchService;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.SEARCH_METADATA;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        List<String> keywords = extractKeywords(ctx.arguments());
        if (keywords.isEmpty())
            throw new ToolExecuteException(ToolError.PARAM_MISSING, "keywords");

        if (ctx.scopeItems().isEmpty())
            return new SearchHit(keywords, List.of(), List.of());

        Set<String> dsIds = dataScopeService.getResolvedDataSourceIds(ctx.serviceId());
        List<String> subjectIds = ctx.scopeItems().stream()
                .filter(s -> s.getScopeType() == McpDataScopeType.SUBJECT)
                .map(McpServiceDataScope::getReferenceId)
                .distinct()
                .toList();

        SearchResult result = semanticSearchService.search(
                keywords, new ArrayList<>(dsIds), subjectIds);

        List<DataSourceDef> dataSources = result.dataSources().stream()
                .map(g -> new DataSourceDef(g.dataSourceId(), g.dataSourceName(),
                        g.dbType(), g.defaultSchema(), g.description(),
                        g.tables().stream()
                                .map(tm -> new TableDef(tm.tableName(), tm.schema(),
                                        tm.description(), tm.aliases(), tm.columns()))
                                .toList()))
                .toList();

        List<TermDef> terms = result.terms().stream()
                .map(t -> new TermDef(t.name(), t.description(), t.subjectName()))
                .toList();

        return new SearchHit(keywords, dataSources, terms);
    }

    private List<String> extractKeywords(Map<String, Object> args) {
        Object val = args.get("keywords");
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }
}
