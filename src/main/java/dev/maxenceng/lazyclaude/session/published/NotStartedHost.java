package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** No hosted session was started; {@code reason} is why, and is the text the status line says too when a warning was raised. */
public record NotStartedHost(Reason reason) implements HostStart {

    public NotStartedHost {
        Assert.notNull("reason", reason);
    }
}
