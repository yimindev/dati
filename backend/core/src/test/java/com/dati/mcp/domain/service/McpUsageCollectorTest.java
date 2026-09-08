package com.dati.mcp.domain.service;

import com.dati.mcp.repository.dao.McpInvocationLogDAO;
import com.dati.mcp.repository.dao.McpUsageDailyStatDAO;
import com.dati.mcp.repository.po.McpInvocationLogPO;
import com.dati.mcp.repository.po.McpUsageDailyStatPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpUsageCollector tests")
class McpUsageCollectorTest {

    @Mock
    private McpInvocationLogDAO logDAO;

    @Mock
    private McpUsageDailyStatDAO dailyStatDAO;

    @Captor
    ArgumentCaptor<List<McpInvocationLogPO>> logsCaptor;

    private McpUsageCollector collector;

    @BeforeEach
    void setUp() {
        // enabled = true, batchSize = 100
        collector = new McpUsageCollector(logDAO, dailyStatDAO, true, 100);
    }

    @Test
    @DisplayName("Should buffer record in memory and update counters")
    void shouldBufferRecordInMemory() {
        collector.record("svc-1", "tools/call", "execute_sql", "user-1", "Admin", "127.0.0.1", true, 45L, null);

        assertThat(collector.getBufferedLogCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should do nothing when collector is disabled")
    void shouldDoNothingWhenDisabled() {
        McpUsageCollector disabledCollector = new McpUsageCollector(logDAO, dailyStatDAO, false, 100);
        disabledCollector.record("svc-1", "tools/call", "execute_sql", "user-1", "Admin", "127.0.0.1", true, 45L, null);

        assertThat(disabledCollector.getBufferedLogCount()).isEqualTo(0);
        disabledCollector.flush();

        verify(logDAO, never()).saveAll(any());
        verify(dailyStatDAO, never()).incrementStat(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should flush buffered logs and increment daily stat when row exists")
    void shouldFlushAndIncrementDailyStatWhenRowExists() {
        collector.record("svc-1", "tools/call", "execute_sql", "user-1", "Admin", "127.0.0.1", true, 45L, null);
        collector.record("svc-1", "tools/call", "execute_sql", "user-1", "Admin", "127.0.0.1", false, 120L, "SQL error");

        when(dailyStatDAO.incrementStat(eq("svc-1"), eq("execute_sql"), eq(LocalDate.now().toString()), eq(2L), eq(1L), eq(1L), eq(165L), eq(120L)))
            .thenReturn(1);

        collector.flush();

        verify(logDAO).saveAll(logsCaptor.capture());
        assertThat(logsCaptor.getValue()).hasSize(2);
        assertThat(collector.getBufferedLogCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should insert new daily stat row when increment returns 0 rows updated")
    void shouldInsertDailyStatWhenRowDoesNotExist() {
        collector.record("svc-1", "tools/call", "execute_sql", "user-1", "Admin", "127.0.0.1", true, 50L, null);

        when(dailyStatDAO.incrementStat(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
            .thenReturn(0);

        collector.flush();

        ArgumentCaptor<McpUsageDailyStatPO> statCaptor = ArgumentCaptor.forClass(McpUsageDailyStatPO.class);
        verify(dailyStatDAO).save(statCaptor.capture());
        McpUsageDailyStatPO savedStat = statCaptor.getValue();
        assertThat(savedStat.getServiceId()).isEqualTo("svc-1");
        assertThat(savedStat.getToolName()).isEqualTo("execute_sql");
        assertThat(savedStat.getTotalCalls()).isEqualTo(1L);
        assertThat(savedStat.getSuccessCalls()).isEqualTo(1L);
        assertThat(savedStat.getFailedCalls()).isEqualTo(0L);
        assertThat(savedStat.getTotalDurationMs()).isEqualTo(50L);
        assertThat(savedStat.getMaxDurationMs()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Should keep today's accumulator after flush and remove it once stale")
    void shouldRemoveStaleAccumulatorAfterDayRollover() {
        collector.record("svc-1", "tools/call", "execute_sql", "user-1", "Admin", "127.0.0.1", true, 45L, null);

        when(dailyStatDAO.incrementStat(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
            .thenReturn(1);

        // Normal flush drains counters but keeps the entry: same-day calls keep accumulating into it
        collector.flushDailyStats(LocalDate.now().toString());
        assertThat(collector.accumulatorEntryCount()).isEqualTo(1);
        verify(dailyStatDAO, times(1)).incrementStat(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());

        // After midnight the entry becomes stale (new records only target today's key): no residual
        // counters are drained, the entry is dropped, and no second DB update happens
        collector.flushDailyStats(LocalDate.now().plusDays(1).toString());
        assertThat(collector.accumulatorEntryCount()).isZero();
        verify(dailyStatDAO, times(1)).incrementStat(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
    }
}
