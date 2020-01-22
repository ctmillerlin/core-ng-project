package core.framework.internal.log;

import core.framework.internal.log.appender.LogAppender;
import core.framework.internal.stat.Stat;
import core.framework.log.message.StatMessage;
import core.framework.util.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * @author neo
 */
public final class CollectStatTask implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(CollectStatTask.class);
    private final LogAppender appender;
    private final Stat stat;

    public CollectStatTask(LogAppender appender, Stat stat) {
        this.appender = appender;
        this.stat = stat;
    }

    @Override
    public void run() {
        Map<String, Double> stats = stat.collect();
        StatMessage message = message(stats);
        try {
            appender.append(message);
        } catch (Throwable e) {
            logger.warn("failed to append stat, error={}", e.getMessage(), e);
        }
    }

    StatMessage message(Map<String, Double> stats) {
        var message = new StatMessage();
        var now = Instant.now();
        message.date = now;
        message.result = "OK";
        message.id = LogManager.ID_GENERATOR.next(now);
        message.app = LogManager.APP_NAME;
        message.host = Network.LOCAL_HOST_NAME;
        message.stats = stats;
        return message;
    }
}
