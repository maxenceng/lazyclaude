package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;
import java.util.concurrent.CompletionException;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.RefusedTicketCaptureView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureView;
import dev.maxenceng.lazyclaude.board.published.Reason;

/** What a capture that threw instead of answering shows as: a refusal that says what it threw, so the popup stays and says why. */
final class CaptureReads {

    private CaptureReads() {}

    static TicketCaptureView refused(Throwable failed) {
        Throwable cause = failed instanceof CompletionException wrapped && wrapped.getCause() != null ? wrapped.getCause() : failed;
        return new RefusedTicketCaptureView(new Reason(Optional.ofNullable(cause.getMessage())
                .filter(message -> !message.isBlank())
                .orElseGet(() -> cause.getClass().getSimpleName())));
    }
}
