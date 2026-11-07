package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.projectPath;

public final class CaptureSidecarFixture {

    private CaptureSidecarFixture() {}

    public static String captureParams() {
        return "{\"root\":\"%s\",\"title\":\"A board from the backlog\"}".formatted(projectPath());
    }

    public static String captured() {
        return "{\"kind\":\"capturedTicket\",\"id\":\"1\"}";
    }

    public static String refusedBecauseOfTheReason() {
        return "{\"kind\":\"refusedTicketCapture\",\"reason\":\"status is missing\"}";
    }

    public static String refusedBecauseGhFailed() {
        return "{\"kind\":\"refusedTicketCapture\",\"reason\":\"gh is not installed\"}";
    }
}
