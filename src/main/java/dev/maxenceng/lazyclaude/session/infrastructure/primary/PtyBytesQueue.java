package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;

/** The pty bytes handed to an emulator, read back as a stream that waits for more until it is closed, so a sequence split between two reads is read whole. */
final class PtyBytesQueue extends InputStream {

    private static final int CLOSED = -1;
    private static final int ONE = 1;
    private static final int FROM_START = 0;
    private static final PtyBytes END = new PtyBytes(new byte[0]);

    private final BlockingQueue<PtyBytes> queue = new LinkedBlockingQueue<>();
    private byte[] current = new byte[0];
    private int position;
    private boolean closed;
    private volatile boolean stopped;

    void offer(PtyBytes bytes) {
        if (!stopped) {
            queue.add(bytes);
        }
    }

    /** Drops what is waiting and whatever is offered from now on, for a reader that will read no more. */
    void stop() {
        stopped = true;
        queue.clear();
    }

    @Override
    public void close() {
        queue.add(END);
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[ONE];
        return read(one, FROM_START, ONE) == CLOSED ? CLOSED : Byte.toUnsignedInt(one[FROM_START]);
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        if (!await()) {
            return CLOSED;
        }
        int count = Math.min(length, current.length - position);
        System.arraycopy(current, position, target, offset, count);
        position += count;
        return count;
    }

    @Override
    public int available() {
        return current.length - position;
    }

    private boolean await() throws IOException {
        closed = closed || stopped;
        while (position == current.length && !closed) {
            try {
                PtyBytes next = queue.take();
                closed = next == END;
                current = next.value();
                position = 0;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException(interrupted);
            }
        }
        return !closed;
    }
}
