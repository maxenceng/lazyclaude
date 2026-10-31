package dev.maxenceng.lazyclaude.sidecar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import tools.jackson.databind.JsonNode;

/** The one writer of stdout: every message is a line, and a single thread writes them, so lines never interleave (ADR 0050). */
final class SidecarOutput implements AutoCloseable {

    private static final String DROPPED = "dropped a message sent after the output closed";
    private static final String CANNOT_WRITE = "cannot write to stdout";
    private static final String WRITER_NAME = "sidecar-writer";
    private static final System.Logger LOG = System.getLogger(SidecarOutput.class.getName());

    private final BufferedWriter out;
    private final ExecutorService writer =
            Executors.newSingleThreadExecutor(task -> Thread.ofPlatform().name(WRITER_NAME).daemon().unstarted(task));

    SidecarOutput(OutputStream out) {
        this.out = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    /** Queues the message; one sent after {@link #close} is dropped, since nobody is reading any more. */
    void send(JsonNode message) {
        String line = SidecarJson.line(message);
        try {
            writer.execute(() -> write(line));
        } catch (RejectedExecutionException closed) {
            LOG.log(System.Logger.Level.DEBUG, DROPPED, closed);
        }
    }

    /** Writes everything queued, then stops. */
    @Override
    public void close() {
        writer.shutdown();
        try {
            writer.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void write(String line) {
        try {
            out.write(line);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, CANNOT_WRITE, e);
        }
    }
}
