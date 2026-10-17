package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What an environment variable is set to; a variable set to nothing is not one. */
public record VariableValue(String value) {

    public VariableValue {
        Assert.notBlank("variable value", value);
    }
}
