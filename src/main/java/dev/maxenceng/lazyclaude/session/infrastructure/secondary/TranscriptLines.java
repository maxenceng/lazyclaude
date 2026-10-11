package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Reads the complete lines of a file from a byte offset; a last line with no newline yet is left for the next read. */
final class TranscriptLines {

    private static final int CHUNK = 64 * 1024;
    private static final byte NEWLINE = '\n';

    private TranscriptLines() {}

    /** Hands each complete line to {@code line}, and returns the offset just after the last one, where the next read starts. */
    static long read(Path file, long from, Consumer<byte[]> line) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file)) {
            channel.position(from);
            return read(Channels.newInputStream(channel), from, line);
        }
    }

    private static long read(InputStream in, long from, Consumer<byte[]> line) throws IOException {
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        byte[] chunk = new byte[CHUNK];
        long consumed = from;
        int count = in.read(chunk);
        while (count != -1) {
            int start = 0;
            for (int i = 0; i < count; i++) {
                if (chunk[i] == NEWLINE) {
                    pending.write(chunk, start, i - start);
                    line.accept(pending.toByteArray());
                    consumed += pending.size() + 1L;
                    pending.reset();
                    start = i + 1;
                }
            }
            pending.write(chunk, start, count - start);
            count = in.read(chunk);
        }
        return consumed;
    }
}
