package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class IssuesWatchFixture {

    private IssuesWatchFixture() {}

    static GhResult answeredWith(String etag) {
        return new GhResult(0, "HTTP/2.0 200 OK\nContent-Type: application/json\nEtag: %s\n\n".formatted(etag).getBytes(StandardCharsets.UTF_8), new byte[0]);
    }

    static GhResult answeredWithoutAnEtag() {
        return new GhResult(0, "HTTP/2.0 200 OK\nContent-Type: application/json\n\n".getBytes(StandardCharsets.UTF_8), new byte[0]);
    }

    static GhResult notModified() {
        return new GhResult(1, "HTTP/2.0 304 Not Modified\nEtag: W/\"a\"\n\n".getBytes(StandardCharsets.UTF_8), "gh: HTTP 304".getBytes(StandardCharsets.UTF_8));
    }

    static GhResult failed() {
        return new GhResult(1, new byte[0], "error connecting to api.github.com".getBytes(StandardCharsets.UTF_8));
    }

    static String firstEtag() {
        return "W/\"a\"";
    }

    static String secondEtag() {
        return "W/\"b\"";
    }

    static Duration everyFewMillis() {
        return Duration.ofMillis(20);
    }

    static Duration seconds() {
        return Duration.ofSeconds(5);
    }
}
