package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;

public final class PtyOutput implements Consumer<PtyBytes> {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration POLL = Duration.ofMillis(20);

    private final ByteArrayOutputStream received = new ByteArrayOutputStream();
    private final List<String> readers = new CopyOnWriteArrayList<>();

    @Override
    public void accept(PtyBytes bytes) {
        readers.add(Thread.currentThread().getName());
        synchronized (received) {
            received.writeBytes(bytes.value());
        }
    }

    public String text() {
        synchronized (received) {
            return received.toString(StandardCharsets.UTF_8);
        }
    }

    public List<String> readerThreads() {
        return List.copyOf(readers);
    }

    public String textContaining(String fragment) throws Exception {
        Instant until = Instant.now().plus(PATIENCE);
        while (!text().contains(fragment) && Instant.now().isBefore(until)) {
            Thread.sleep(POLL.toMillis());
        }
        if (!text().contains(fragment)) {
            throw new TimeoutException("the child never wrote %s; it wrote:%n%s".formatted(fragment, text()));
        }
        return text();
    }
}
