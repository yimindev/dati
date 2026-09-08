package com.dati.mcp.server.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class McpUsageStatsVO {

    private long totalCalls;

    private long todayCalls;

    private double successRate;

    private double avgDurationMs;

    private List<DailyStatItem> dailyTrend = new ArrayList<>();

    private List<ToolStatItem> toolDistribution = new ArrayList<>();

    @Data
    public static class DailyStatItem {

        private String date;

        private long totalCalls;

        private long successCalls;

        private long failedCalls;

        private double successRate;

        private double avgDurationMs;
    }

    @Data
    public static class ToolStatItem {

        private String toolName;

        private long callCount;

        private double percentage;

        private double successRate;
    }
}
