package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.TicketCaptureRefusedException;

/** Tells why a {@code gh} run that ended non-zero could not read or write the issues, from what it said: no GitHub remote, not logged in, or the rest as unreachable. */
final class GhFailure {

    private static final Pattern NO_REMOTE = Pattern.compile("no git remotes found|none of the git remotes|not a git repository");
    private static final Pattern NOT_LOGGED_IN = Pattern.compile("gh auth login|not logged in");
    private static final String NO_REMOTE_REASON = "this project has no GitHub remote, so it has no backlog";
    private static final String NOT_LOGGED_IN_REASON = "gh is not logged in; run gh auth login";
    private static final String UNREADABLE_REASON = "cannot read the issues from GitHub: %s";
    private static final String UNWRITABLE_REASON = "cannot create the ticket on GitHub: %s";

    private GhFailure() {}

    static BacklogUnavailableException of(GhResult failed) {
        return new BacklogUnavailableException(reason(failed, UNREADABLE_REASON));
    }

    static TicketCaptureRefusedException refusing(GhResult failed) {
        return new TicketCaptureRefusedException(reason(failed, UNWRITABLE_REASON));
    }

    /** {@code unreachable} words the rest, with what {@code gh} said first. */
    private static String reason(GhResult failed, String unreachable) {
        String said = failed.saidText();
        if (NO_REMOTE.matcher(said).find()) {
            return NO_REMOTE_REASON;
        }
        if (NOT_LOGGED_IN.matcher(said).find()) {
            return NOT_LOGGED_IN_REASON;
        }
        return unreachable.formatted(failed.firstLineSaid());
    }
}
