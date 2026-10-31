package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class CapturedLog extends Handler {

    private final Logger logger;
    private final List<LogRecord> records = new CopyOnWriteArrayList<>();

    private CapturedLog(Logger logger) {
        this.logger = logger;
    }

    public static CapturedLog of(Class<?> logging) {
        CapturedLog captured = new CapturedLog(Logger.getLogger(logging.getName()));
        captured.logger.addHandler(captured);
        return captured;
    }

    public List<Throwable> thrown() {
        return records.stream().map(LogRecord::getThrown).filter(Objects::nonNull).toList();
    }

    @Override
    public void publish(LogRecord logged) {
        records.add(logged);
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        logger.removeHandler(this);
    }
}
