package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/** Reads the complete lines of a file from a byte offset; a last line with no newline yet is left for the next read. */
final class TranscriptLines {

    private static final int CHUNK = 64 * 1024;
    private static final byte NEWLINE = '\n';
    private static final int END_OF_STREAM = -1;

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
        long chunkStart = from;
        int count = in.read(chunk);
        while (count != END_OF_STREAM) {
            int[] newlines = newlinesIn(chunk, count);
            handOver(chunk, newlines, pending, line);
            int rest = newlines.length == 0 ? 0 : newlines[newlines.length - 1] + 1;
            consumed = newlines.length == 0 ? consumed : chunkStart + rest;
            pending.write(chunk, rest, count - rest);
            chunkStart += count;
            count = in.read(chunk);
        }
        return consumed;
    }

    private static int[] newlinesIn(byte[] chunk, int count) {
        return IntStream.range(0, count).filter(index -> chunk[index] == NEWLINE).toArray();
    }

    /** Each line ending at one of {@code newlines}, the first carrying whatever an earlier chunk left pending. */
    private static void handOver(byte[] chunk, int[] newlines, ByteArrayOutputStream pending, Consumer<byte[]> line) {
        IntStream.range(0, newlines.length).forEach(nth -> {
            int start = nth == 0 ? 0 : newlines[nth - 1] + 1;
            pending.write(chunk, start, newlines[nth] - start);
            line.accept(pending.toByteArray());
            pending.reset();
        });
    }
}
