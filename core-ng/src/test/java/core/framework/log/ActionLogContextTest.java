package core.framework.log;

import core.framework.internal.log.LogManager;
import core.framework.internal.log.Trace;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class ActionLogContextTest {
    @Test
    void withoutCurrentActionLog() {
        assertThat(ActionLogContext.id()).isNull();

        ActionLogContext.put("key", "value");
        assertThat(ActionLogContext.get("key")).isEmpty();

        ActionLogContext.stat("stat", 1);

        assertThat(ActionLogContext.track("db", 100)).isEqualTo(1);
    }

    @Test
    void withCurrentActionLog() {
        var logManager = new LogManager();
        logManager.begin("begin", null);

        assertThat(ActionLogContext.id()).isNotNull();

        assertThat(ActionLogContext.get("key")).isEmpty();
        ActionLogContext.put("key", "value");
        assertThat(ActionLogContext.get("key")).contains("value");

        String value = null;
        ActionLogContext.put("nullValue", value);
        assertThat(ActionLogContext.get("nullValue")).contains("null");

        assertThat(ActionLogContext.track("db", 100)).isEqualTo(1);
        assertThat(ActionLogContext.track("db", 100)).isEqualTo(2);

        logManager.end("end");
    }

    @Test
    void trace() {
        ActionLogContext.triggerTrace(true);

        var logManager = new LogManager();
        logManager.begin("begin", null);
        ActionLogContext.triggerTrace(false);
        assertThat(LogManager.CURRENT_ACTION_LOG.get().trace).isEqualTo(Trace.CURRENT);
        logManager.end("end");
    }

    @Test
    void remainingProcessTime() {
        assertThat(ActionLogContext.remainingProcessTime()).isNull();

        var logManager = new LogManager();
        logManager.begin("begin", null);
        ActionLogContext.maxProcessTime(Duration.ofMinutes(30));
        assertThat(ActionLogContext.remainingProcessTime()).isGreaterThan(Duration.ofMinutes(20));
        logManager.end("end");
    }
}
