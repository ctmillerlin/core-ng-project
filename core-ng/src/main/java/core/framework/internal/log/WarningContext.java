package core.framework.internal.log;

import core.framework.log.IOWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;

import static core.framework.log.Markers.errorCode;

/**
 * @author neo
 */
public final class WarningContext {
    // use static constant mapping for both performance and simplicity
    static final Map<String, PerformanceWarning> DEFAULT_WARNINGS = Map.of(
        "db", new PerformanceWarning("db", 2000, Duration.ofSeconds(5), 2000, 10_000, 10_000),
        "redis", new PerformanceWarning("redis", 2000, Duration.ofMillis(500), 1000, 10_000, 10_000),
        "elasticsearch", new PerformanceWarning("elasticsearch", 2000, Duration.ofSeconds(5), 2000, 10_000, 10_000),
        "mongo", new PerformanceWarning("mongo", 2000, Duration.ofSeconds(5), 2000, 10_000, 10_000)
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(WarningContext.class);

    @Nullable
    public static PerformanceWarning[] warnings(IOWarning[] warnings) {
        if (warnings.length <= 0) return null;
        var results = new PerformanceWarning[warnings.length];
        for (int i = 0; i < warnings.length; i++) {
            IOWarning warning = warnings[i];
            String operation = warning.operation();
            PerformanceWarning defaultWarning = DEFAULT_WARNINGS.get(operation);

            int maxOperations = warning.maxOperations();
            if (maxOperations < 0 && defaultWarning != null) maxOperations = defaultWarning.maxOperations;

            Duration maxElapsed = null;
            if (warning.maxElapsedInMs() > 0) maxElapsed = Duration.ofMillis(warning.maxElapsedInMs());
            if (maxElapsed == null && defaultWarning != null && defaultWarning.maxElapsed > 0) maxElapsed = Duration.ofNanos(defaultWarning.maxElapsed);

            int maxReads = warning.maxReads();
            if (maxReads < 0 && defaultWarning != null) maxReads = defaultWarning.maxReads;

            int maxTotalReads = warning.maxTotalReads();
            if (maxTotalReads < 0 && defaultWarning != null) maxTotalReads = defaultWarning.maxTotalReads;

            int maxTotalWrites = warning.maxTotalWrites();
            if (maxTotalWrites < 0 && defaultWarning != null) maxTotalWrites = defaultWarning.maxTotalWrites;
            results[i] = new PerformanceWarning(operation, maxOperations, maxElapsed, maxReads, maxTotalReads, maxTotalWrites);
        }
        return results;
    }

    public boolean suppressSlowSQLWarning;
    long maxProcessTimeInNano;

    public void maxProcessTimeInNano(long maxProcessTimeInNano) {
        this.maxProcessTimeInNano = maxProcessTimeInNano;
        LOGGER.debug("maxProcessTime={}", maxProcessTimeInNano);
    }

    public void checkMaxProcessTime(long elapsed) {
        if (maxProcessTimeInNano > 0 && elapsed > maxProcessTimeInNano) {
            LOGGER.warn(errorCode("SLOW_PROCESS"), "action took longer than of max process time, maxProcessTime={}, elapsed={}", Duration.ofNanos(maxProcessTimeInNano), Duration.ofNanos(elapsed));
        }
    }
}
