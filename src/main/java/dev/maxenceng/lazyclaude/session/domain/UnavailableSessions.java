package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * Neither the registry nor {@code claude agents --json} answered; lazyclaude starts anyway and says why. The past sessions
 * are not shown either: without the live ones, a session still running cannot be told from its past row.
 */
public record UnavailableSessions(Reason reason) implements Sessions {

    public UnavailableSessions {
        Assert.notNull("reason", reason);
    }
}
