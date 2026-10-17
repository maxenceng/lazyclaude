package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The name of an environment variable. */
public record VariableName(String value) {

    public VariableName {
        Assert.notBlank("variable name", value);
    }
}
