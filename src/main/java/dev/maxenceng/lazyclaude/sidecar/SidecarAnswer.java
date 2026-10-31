package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;

/**
 * What a method answers: its result, and what to do once that result is queued, so a notification it starts can never overtake the
 * response it follows.
 */
record SidecarAnswer(JsonNode result, Runnable afterwards) {

    private static final Runnable NOTHING = () -> {};

    static SidecarAnswer of(JsonNode result) {
        return new SidecarAnswer(result, NOTHING);
    }
}
