package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.SessionAge;

/** A session's age as its row draws it. */
public record SessionAgeView(SessionAge age) {

    public SessionAgeView {
        Assert.notNull("age", age);
    }

    public String label() {
        return age.label();
    }
}
