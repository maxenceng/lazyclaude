package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.nio.file.Path;

/** What {@link TicketCaptureCommand} is asked with: the project's root and the title as typed. */
public record TicketCapturePayload(Path root, String title) {}
