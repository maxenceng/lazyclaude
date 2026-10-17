package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What an environment variable is set to; a variable set to nothing is not one. */
public record EnvironmentVariableValue(String value) {

    public EnvironmentVariableValue {
        Assert.notBlank("environment variable value", value);
    }
}
