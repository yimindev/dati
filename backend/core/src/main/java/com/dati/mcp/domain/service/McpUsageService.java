package com.dati.mcp.domain.service;

import com.dati.mcp.repository.dao.McpInvocationLogDAO;
import com.dati.mcp.repository.dao.McpUsageDailyStatDAO;
import com.dati.mcp.repository.po.McpInvocationLogPO;
import com.dati.mcp.repository.po.McpUsageDailyStatPO;
import com.dati.mcp.server.pojo.McpUsageStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class McpUsageService {

    private final McpInvocationLogDAO logDAO;
    private final McpUsageDailyStatDAO dailyStatDAO;
    private final int retentionDays;

    public McpUsageService(McpInvocationLogDAO logDAO,
                           McpUsageDailyStatDAO dailyStatDAO,
                           @Value("${dati.mcp.stats.retention-days:15}") int retentionDays) {
        this.logDAO = logDAO;
        this.dailyStatDAO = dailyStatDAO;
        this.retentionDays = retentionDays > 0 ? retentionDays : 15;
    }

    /**
     * Get usage statistics for a service over the last N days (default 15).
     */
    public McpUsageStatsVO getUsageStats(String serviceId, int days) {
        int queryDays = days > 0 ? days : retentionDays;
        String sinceDate = LocalDate.now().minusDays(queryDays).toString();
        List<McpUsageDailyStatPO> dailyList = dailyStatDAO
            .findByServiceIdAndStatDateGreaterThanEqualOrderByStatDateAsc(serviceId, sinceDate);

        McpUsageStatsVO vo = new McpUsageStatsVO();
        if (dailyList.isEmpty()) {
            vo.setSuccessRate(100.0);
            return vo;
        }

        String today = LocalDate.now().toString();
        long totalCalls = 0L;
        long totalSuccess = 0L;
        long totalDuration = 0L;
        long todayCalls = 0L;

        // Group by stat_date for daily trend
        Map<String, List<McpUsageDailyStatPO>> byDate = dailyList.stream()
            .collect(Collectors.groupingBy(McpUsageDailyStatPO::getStatDate, LinkedHashMap::new, Collectors.toList()));

        List<McpUsageStatsVO.DailyStatItem> dailyTrend = new ArrayList<>();
        for (var entry : byDate.entrySet()) {
            String date = entry.getKey();
            List<McpUsageDailyStatPO> dateStats = entry.getValue();

            long dayTotal = dateStats.stream().mapToLong(McpUsageDailyStatPO::getTotalCalls).sum();
            long daySuccess = dateStats.stream().mapToLong(McpUsageDailyStatPO::getSuccessCalls).sum();
            long dayFailed = dateStats.stream().mapToLong(McpUsageDailyStatPO::getFailedCalls).sum();
            long dayDuration = dateStats.stream().mapToLong(McpUsageDailyStatPO::getTotalDurationMs).sum();

            totalCalls += dayTotal;
            totalSuccess += daySuccess;
            totalDuration += dayDuration;
            if (today.equals(date)) {
                todayCalls += dayTotal;
            }

            McpUsageStatsVO.DailyStatItem item = new McpUsageStatsVO.DailyStatItem();
            item.setDate(date);
            item.setTotalCalls(dayTotal);
            item.setSuccessCalls(daySuccess);
            item.setFailedCalls(dayFailed);
            item.setSuccessRate(dayTotal > 0 ? roundOneDecimal((double) daySuccess / dayTotal * 100.0) : 100.0);
            item.setAvgDurationMs(dayTotal > 0 ? roundOneDecimal((double) dayDuration / dayTotal) : 0.0);
            dailyTrend.add(item);
        }
        dailyTrend.sort(Comparator.comparing(McpUsageStatsVO.DailyStatItem::getDate));

        // Group by tool_name for tool distribution
        Map<String, List<McpUsageDailyStatPO>> byTool = dailyList.stream()
            .collect(Collectors.groupingBy(McpUsageDailyStatPO::getToolName));

        List<McpUsageStatsVO.ToolStatItem> toolDistribution = new ArrayList<>();
        final long allCalls = totalCalls;
        for (var entry : byTool.entrySet()) {
            String toolName = entry.getKey();
            List<McpUsageDailyStatPO> toolStats = entry.getValue();

            long toolTotal = toolStats.stream().mapToLong(McpUsageDailyStatPO::getTotalCalls).sum();
            long toolSuccess = toolStats.stream().mapToLong(McpUsageDailyStatPO::getSuccessCalls).sum();

            McpUsageStatsVO.ToolStatItem item = new McpUsageStatsVO.ToolStatItem();
            item.setToolName(toolName.isEmpty() ? "General" : toolName);
            item.setCallCount(toolTotal);
            item.setPercentage(allCalls > 0 ? roundOneDecimal((double) toolTotal / allCalls * 100.0) : 0.0);
            item.setSuccessRate(toolTotal > 0 ? roundOneDecimal((double) toolSuccess / toolTotal * 100.0) : 100.0);
            toolDistribution.add(item);
        }
        toolDistribution.sort(Comparator.comparingLong(McpUsageStatsVO.ToolStatItem::getCallCount).reversed());

        vo.setTotalCalls(totalCalls);
        vo.setTodayCalls(todayCalls);
        vo.setSuccessRate(totalCalls > 0 ? roundOneDecimal((double) totalSuccess / totalCalls * 100.0) : 100.0);
        vo.setAvgDurationMs(totalCalls > 0 ? roundOneDecimal((double) totalDuration / totalCalls) : 0.0);
        vo.setDailyTrend(dailyTrend);
        vo.setToolDistribution(toolDistribution);

        return vo;
    }

    /**
     * List invocation logs with pagination and optional filtering.
     */
    public Page<McpInvocationLogPO> listLogs(String serviceId, String toolName, Boolean success, Pageable pageable) {
        return logDAO.findLogs(serviceId, toolName, success, pageable);
    }

    /**
     * Scheduled cleanup of invocation logs and daily stats older than the retention window
     * (runs daily at 3:00 AM).
     *
     * @return total number of deleted records
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public int cleanupExpiredData() {
        Instant logCutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        String statCutoff = LocalDate.now().minusDays(retentionDays).toString();
        log.info("Cleaning up MCP usage data older than {} (retention: {} days)", logCutoff, retentionDays);
        try {
            int logDeleted = logDAO.deleteByCreatedAtBefore(logCutoff);
            int statDeleted = dailyStatDAO.deleteByStatDateBefore(statCutoff);
            log.info("Cleaned up {} expired MCP invocation logs and {} daily stat rows", logDeleted, statDeleted);
            return logDeleted + statDeleted;
        } catch (Exception e) {
            log.error("Failed to clean up expired MCP usage data: {}", e.getMessage(), e);
            return 0;
        }
    }

    private double roundOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
