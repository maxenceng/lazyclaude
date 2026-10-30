package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** Tells why a {@code gh} run that ended non-zero could not read the issues, from what it said: no GitHub remote, not logged in, or the rest as unreachable. */
final class GhFailure {

    private static final Pattern NO_REMOTE = Pattern.compile("no git remotes found|none of the git remotes|not a git repository");
    private static final Pattern NOT_LOGGED_IN = Pattern.compile("gh auth login|not logged in");
    private static final String NOT_LOGGED_IN_REASON = "gh is not logged in; run gh auth login";
    private static final String UNREACHABLE_REASON = "cannot read the issues from GitHub: %s";

    private GhFailure() {}

    static DomainException of(GhResult failed) {
        String said = failed.saidText();
        if (NO_REMOTE.matcher(said).find()) {
            return new BacklogNotFoundException();
        }
        if (NOT_LOGGED_IN.matcher(said).find()) {
            return new BacklogUnavailableException(NOT_LOGGED_IN_REASON);
        }
        return new BacklogUnavailableException(UNREACHABLE_REASON.formatted(failed.firstLineSaid()));
    }
}
