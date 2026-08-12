package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.param.SearchMetadataArgs;
import com.dati.mcp.server.pojo.SearchHit;
import com.dati.semantic.domain.service.SemanticSearchService;
import com.dati.semantic.domain.service.SemanticSearchService.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchMetadataExecutor tests")
class SearchMetadataExecutorTest {

    @Mock
    private McpServiceDataScopeService dataScopeService;

    @Mock
    private SemanticSearchService semanticSearchService;

    private SearchMetadataExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new SearchMetadataExecutor(dataScopeService, semanticSearchService);
    }

    private ToolExecutionContext ctx(SearchMetadataArgs args, List<McpServiceDataScope> scopes) {
        return new ToolExecutionContext("svc-1", McpToolType.SEARCH_METADATA,
            new ToolConfig.SearchMetadataConfig(), args, scopes);
    }

    @Test
    @DisplayName("searches with bound keywords when scope is present")
    void searchesWithBoundKeywords() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));
        when(semanticSearchService.search(List.of("orders"), new ArrayList<>(Set.of("ds-1")), List.of()))
            .thenReturn(new SearchResult(List.of(), List.of()));

        SearchHit hit = (SearchHit) executor.execute(ctx(new SearchMetadataArgs(List.of("orders")), List.of(scope)));

        assertThat(hit.keywords()).containsExactly("orders");
        verify(semanticSearchService).search(eq(List.of("orders")), any(), eq(List.of()));
    }

    @Test
    @DisplayName("empty scope returns empty result without searching")
    void emptyScopeReturnsEmpty() {
        SearchHit hit = (SearchHit) executor.execute(ctx(new SearchMetadataArgs(List.of("orders")), List.of()));

        assertThat(hit.dataSources()).isEmpty();
        assertThat(hit.terms()).isEmpty();
        verifyNoInteractions(semanticSearchService);
    }
}
