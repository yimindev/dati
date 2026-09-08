package com.dati.mcp.domain.service;

import com.dati.mcp.repository.dao.McpInvocationLogDAO;
import com.dati.mcp.repository.dao.McpUsageDailyStatDAO;
import com.dati.mcp.repository.po.McpInvocationLogPO;
import com.dati.mcp.repository.po.McpUsageDailyStatPO;
import com.dati.mcp.server.pojo.McpUsageStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpUsageService tests")
class McpUsageServiceTest {

    @Mock
    private McpInvocationLogDAO logDAO;

    @Mock
    private McpUsageDailyStatDAO dailyStatDAO;

    private McpUsageService usageService;

    @BeforeEach
    void setUp() {
        usageService = new McpUsageService(logDAO, dailyStatDAO, 15);
    }

    @Test
    @DisplayName("Should return empty stats when no daily records found")
    void shouldReturnEmptyStatsWhenNoRecords() {
        when(dailyStatDAO.findByServiceIdAndStatDateGreaterThanEqualOrderByStatDateAsc(eq("svc-1"), any()))
            .thenReturn(List.of());

        McpUsageStatsVO stats = usageService.getUsageStats("svc-1", 15);

        assertThat(stats.getTotalCalls()).isZero();
        assertThat(stats.getTodayCalls()).isZero();
        assertThat(stats.getSuccessRate()).isEqualTo(100.0);
        assertThat(stats.getAvgDurationMs()).isZero();
        assertThat(stats.getDailyTrend()).isEmpty();
        assertThat(stats.getToolDistribution()).isEmpty();
    }

    @Test
    @DisplayName("Should aggregate daily records into stats, daily trend, and tool distribution")
    void shouldAggregateStatsCorrectly() {
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();

        McpUsageDailyStatPO stat1 = new McpUsageDailyStatPO();
        stat1.setServiceId("svc-1");
        stat1.setToolName("execute_sql");
        stat1.setStatDate(yesterday);
        stat1.setTotalCalls(10L);
        stat1.setSuccessCalls(9L);
        stat1.setFailedCalls(1L);
        stat1.setTotalDurationMs(500L);
        stat1.setMaxDurationMs(120L);

        McpUsageDailyStatPO stat2 = new McpUsageDailyStatPO();
        stat2.setServiceId("svc-1");
        stat2.setToolName("execute_sql");
        stat2.setStatDate(today);
        stat2.setTotalCalls(5L);
        stat2.setSuccessCalls(5L);
        stat2.setFailedCalls(0L);
        stat2.setTotalDurationMs(200L);
        stat2.setMaxDurationMs(50L);

        McpUsageDailyStatPO stat3 = new McpUsageDailyStatPO();
        stat3.setServiceId("svc-1");
        stat3.setToolName("list_tables");
        stat3.setStatDate(today);
        stat3.setTotalCalls(5L);
        stat3.setSuccessCalls(5L);
        stat3.setFailedCalls(0L);
        stat3.setTotalDurationMs(100L);
        stat3.setMaxDurationMs(30L);

        when(dailyStatDAO.findByServiceIdAndStatDateGreaterThanEqualOrderByStatDateAsc(eq("svc-1"), any()))
            .thenReturn(List.of(stat1, stat2, stat3));

        McpUsageStatsVO stats = usageService.getUsageStats("svc-1", 15);

        assertThat(stats.getTotalCalls()).isEqualTo(20L);
        assertThat(stats.getTodayCalls()).isEqualTo(10L); // stat2 (5) + stat3 (5)
        assertThat(stats.getSuccessRate()).isEqualTo(95.0); // 19 / 20 = 95.0%
        assertThat(stats.getAvgDurationMs()).isEqualTo(40.0); // 800 / 20 = 40.0ms

        // Daily trend verification
        assertThat(stats.getDailyTrend()).hasSize(2);
        assertThat(stats.getDailyTrend().get(0).getDate()).isEqualTo(yesterday);
        assertThat(stats.getDailyTrend().get(0).getTotalCalls()).isEqualTo(10L);
        assertThat(stats.getDailyTrend().get(1).getDate()).isEqualTo(today);
        assertThat(stats.getDailyTrend().get(1).getTotalCalls()).isEqualTo(10L);

        // Tool distribution verification
        assertThat(stats.getToolDistribution()).hasSize(2);
        var execSqlTool = stats.getToolDistribution().stream()
            .filter(t -> "execute_sql".equals(t.getToolName()))
            .findFirst().orElseThrow();
        assertThat(execSqlTool.getCallCount()).isEqualTo(15L);
        assertThat(execSqlTool.getPercentage()).isEqualTo(75.0); // 15 / 20 = 75.0%
    }

    @Test
    @DisplayName("Should list logs with pagination and filters")
    void shouldListLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        McpInvocationLogPO po = new McpInvocationLogPO();
        po.setServiceId("svc-1");
        po.setToolName("execute_sql");
        Page<McpInvocationLogPO> page = new PageImpl<>(List.of(po));

        when(logDAO.findLogs("svc-1", "execute_sql", true, pageable)).thenReturn(page);

        Page<McpInvocationLogPO> result = usageService.listLogs("svc-1", "execute_sql", true, pageable);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should clean up expired logs and daily stats older than retention days")
    void shouldCleanupExpiredData() {
        when(logDAO.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(5);
        when(dailyStatDAO.deleteByStatDateBefore(anyString())).thenReturn(3);

        int deleted = usageService.cleanupExpiredData();

        assertThat(deleted).isEqualTo(8);
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(logDAO).deleteByCreatedAtBefore(captor.capture());
        // Verify cutoff is around 15 days ago
        Instant cutoff = captor.getValue();
        Instant expected = Instant.now().minus(15, ChronoUnit.DAYS);
        assertThat(cutoff).isBetween(expected.minus(1, ChronoUnit.MINUTES), expected.plus(1, ChronoUnit.MINUTES));

        ArgumentCaptor<String> dateCaptor = ArgumentCaptor.forClass(String.class);
        verify(dailyStatDAO).deleteByStatDateBefore(dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(15).toString());
    }
}
