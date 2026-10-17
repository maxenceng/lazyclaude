package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The name of an environment variable. */
public record EnvironmentVariableName(String value) {

    public EnvironmentVariableName {
        Assert.notBlank("environment variable name", value);
    }
}
