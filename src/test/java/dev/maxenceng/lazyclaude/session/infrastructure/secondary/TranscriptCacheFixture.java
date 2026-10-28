package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.List;

public final class TranscriptCacheFixture {

    private TranscriptCacheFixture() {}

    static CachedTranscript cachedTranscript() {
        return new CachedTranscript(
                "/home/max/.claude/projects/-work-app/3f2a91c0-5d4e-4b8a-9c11-0e7d2a6b8f10.jsonl",
                1024,
                1_760_000_000_000L,
                "(dev=803,ino=4242)",
                1000,
                "3f2a91c0-5d4e-4b8a-9c11-0e7d2a6b8f10",
                "Refine PROJ-1",
                "/ticket start PROJ-1",
                1_760_173_920_000L,
                List.of(new CachedTicketRun("start", List.of("PROJ-1"))));
    }

    static CachedTranscript otherCachedTranscript() {
        return new CachedTranscript(
                "/home/max/.claude/projects/-work-app/9b1e44d2-77aa-4c3f-8d20-5a6c0b1f2e33.jsonl",
                2048,
                1_760_000_100_000L,
                "(dev=803,ino=4243)",
                2048,
                "9b1e44d2-77aa-4c3f-8d20-5a6c0b1f2e33",
                null,
                null,
                1_760_110_000_000L,
                List.of());
    }
}
