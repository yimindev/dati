package com.dati.mcp.domain.service;

import com.dati.mcp.repository.dao.McpInvocationLogDAO;
import com.dati.mcp.repository.dao.McpUsageDailyStatDAO;
import com.dati.mcp.repository.po.McpInvocationLogPO;
import com.dati.mcp.repository.po.McpUsageDailyStatPO;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * High-performance in-memory micro-batch collector for MCP usage statistics.
 * Request path performs lock-free atomic in-memory aggregation and log queuing (< 0.1ms).
 * Background scheduled task periodically flushes buffered logs and increments daily statistics in batches.
 */
@Slf4j
@Component
public class McpUsageCollector {

    private static final int MAX_QUEUE_CAPACITY = 10000;
    private static final int MAX_ERROR_LENGTH = 500;

    private final McpInvocationLogDAO logDAO;
    private final McpUsageDailyStatDAO dailyStatDAO;
    private final boolean enabled;
    private final int batchSize;

    private final ConcurrentLinkedQueue<McpInvocationLogPO> logQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<StatKey, StatAccumulator> accumulators = new ConcurrentHashMap<>();
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    public record StatKey(String serviceId, String toolName, String statDate) {
    }

    public static class StatAccumulator {
        private final LongAdder total = new LongAdder();
        private final LongAdder success = new LongAdder();
        private final LongAdder failed = new LongAdder();
        private final LongAdder duration = new LongAdder();
        private final AtomicLong maxDuration = new AtomicLong(0);

        public void record(boolean isSuccess, long durationMs) {
            total.increment();
            if (isSuccess) {
                success.increment();
            } else {
                failed.increment();
            }
            duration.add(durationMs);
            maxDuration.accumulateAndGet(durationMs, Math::max);
        }

        public DeltaSnapshot drain() {
            long deltaTotal = total.sumThenReset();
            if (deltaTotal == 0) {
                return null;
            }
            long deltaSuccess = success.sumThenReset();
            long deltaFailed = failed.sumThenReset();
            long deltaDuration = duration.sumThenReset();
            long max = maxDuration.getAndSet(0);
            return new DeltaSnapshot(deltaTotal, deltaSuccess, deltaFailed, deltaDuration, max);
        }

        /** Re-accumulate a drained snapshot (used to retry after a failed DB flush). */
        public void mergeBack(DeltaSnapshot delta) {
            total.add(delta.total());
            success.add(delta.success());
            failed.add(delta.failed());
            duration.add(delta.duration());
            maxDuration.accumulateAndGet(delta.maxDuration(), Math::max);
        }
    }

    public record DeltaSnapshot(long total, long success, long failed, long duration, long maxDuration) {
    }

    public McpUsageCollector(McpInvocationLogDAO logDAO,
                             McpUsageDailyStatDAO dailyStatDAO,
                             @Value("${dati.mcp.stats.enabled:true}") boolean enabled,
                             @Value("${dati.mcp.stats.batch-size:100}") int batchSize) {
        this.logDAO = logDAO;
        this.dailyStatDAO = dailyStatDAO;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    public void record(String serviceId, String method, String toolName,
                       String callerId, String callerName, String clientIp,
                       boolean success, long durationMs, String errorMessage) {
        if (!enabled || serviceId == null) {
            return;
        }

        try {
            String safeToolName = toolName == null ? "" : toolName;
            String statDate = LocalDate.now().toString();

            // 1. Accumulate in-memory counters (nanosecond-level lock-free CAS)
            StatKey key = new StatKey(serviceId, safeToolName, statDate);
            accumulators.computeIfAbsent(key, k -> new StatAccumulator()).record(success, durationMs);

            // 2. Buffer raw invocation log
            if (logQueue.size() < MAX_QUEUE_CAPACITY) {
                McpInvocationLogPO po = new McpInvocationLogPO();
                po.setServiceId(serviceId);
                po.setMethod(method != null ? method : "");
                po.setToolName(safeToolName);
                po.setCallerId(callerId);
                po.setCallerName(callerName);
                po.setClientIp(clientIp);
                po.setSuccess(success);
                po.setDurationMs(durationMs);
                po.setStatDate(statDate);
                if (errorMessage != null && !errorMessage.isBlank()) {
                    po.setErrorMessage(errorMessage.length() > MAX_ERROR_LENGTH
                        ? errorMessage.substring(0, MAX_ERROR_LENGTH)
                        : errorMessage);
                }
                logQueue.offer(po);
            } else {
                log.warn("MCP usage log queue is full, dropping detailed log for service {}", serviceId);
            }

            // 3. Eager flush if batch size threshold is reached
            if (logQueue.size() >= batchSize) {
                Thread.startVirtualThread(this::flush);
            }
        } catch (Exception e) {
            log.warn("Failed to record MCP usage for service {}: {}", serviceId, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${dati.mcp.stats.flush-interval-seconds:10}", timeUnit = TimeUnit.SECONDS)
    public void flush() {
        if (!flushing.compareAndSet(false, true)) {
            return;
        }
        try {
            flushLogs();
            flushDailyStats();
        } catch (Exception e) {
            log.warn("Failed to flush MCP usage buffer: {}", e.getMessage(), e);
        } finally {
            flushing.set(false);
        }
    }

    private void flushLogs() {
        if (logQueue.isEmpty()) {
            return;
        }
        List<McpInvocationLogPO> batch = new ArrayList<>(batchSize);
        McpInvocationLogPO item;
        while ((item = logQueue.poll()) != null) {
            batch.add(item);
            if (batch.size() >= batchSize) {
                logDAO.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            logDAO.saveAll(batch);
        }
    }

    private void flushDailyStats() {
        flushDailyStats(LocalDate.now().toString());
    }

    /**
     * Drain in-memory counters into the daily stat table.
     *
     * <p>Records are always stamped with the current date, so an entry whose {@code statDate}
     * differs from {@code today} can never receive new records again. Once its residual counters
     * are flushed (or proven empty) it is removed from the map, keeping the accumulator map
     * bounded to the currently active day instead of growing forever.
     * The {@code today} parameter is package-private so tests can simulate day rollover;
     * production always passes {@link LocalDate#now()}.
     */
    void flushDailyStats(String today) {
        if (accumulators.isEmpty()) {
            return;
        }
        for (var entry : accumulators.entrySet()) {
            StatKey key = entry.getKey();
            StatAccumulator accumulator = entry.getValue();
            DeltaSnapshot delta = accumulator.drain();
            boolean stale = !today.equals(key.statDate());
            if (delta == null || delta.total() == 0) {
                if (stale) {
                    accumulators.remove(key, accumulator);
                }
                continue;
            }
            try {
                int updated = dailyStatDAO.incrementStat(
                    key.serviceId(), key.toolName(), key.statDate(),
                    delta.total(), delta.success(), delta.failed(), delta.duration(), delta.maxDuration()
                );
                if (updated == 0) {
                    McpUsageDailyStatPO newRow = new McpUsageDailyStatPO();
                    newRow.setServiceId(key.serviceId());
                    newRow.setToolName(key.toolName());
                    newRow.setStatDate(key.statDate());
                    newRow.setTotalCalls(delta.total());
                    newRow.setSuccessCalls(delta.success());
                    newRow.setFailedCalls(delta.failed());
                    newRow.setTotalDurationMs(delta.duration());
                    newRow.setMaxDurationMs(delta.maxDuration());
                    try {
                        dailyStatDAO.save(newRow);
                    } catch (Exception ex) {
                        // In case of concurrent insert race, fallback to increment
                        dailyStatDAO.incrementStat(
                            key.serviceId(), key.toolName(), key.statDate(),
                            delta.total(), delta.success(), delta.failed(), delta.duration(), delta.maxDuration()
                        );
                    }
                }
                if (stale) {
                    accumulators.remove(key, accumulator);
                }
            } catch (Exception e) {
                // Re-accumulate the drained delta so the next flush retries it
                accumulator.mergeBack(delta);
                log.warn("Failed to increment daily stat for service {} tool {}: {}", key.serviceId(), key.toolName(), e.getMessage());
            }
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Flushing MCP usage stats on shutdown...");
        flush();
    }

    public int getBufferedLogCount() {
        return logQueue.size();
    }

    /** Number of currently tracked per-day accumulators (package-private test hook). */
    int accumulatorEntryCount() {
        return accumulators.size();
    }
}
