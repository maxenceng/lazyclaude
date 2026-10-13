package dev.maxenceng.lazyclaude.review.domain;

import java.util.Comparator;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The checks on a pull request's head. They list worst first, so the first is the CI result: failed over pending over passed. */
public record Checks(List<Check> checks) {

    private static final Comparator<Check> WORST_FIRST =
            Comparator.comparing(Check::outcome).thenComparing(Check::name);

    public Checks {
        Assert.notNull("checks", checks);
    }

    /** The checks failed first, then pending, then passed, by name within each. */
    public List<Check> listed() {
        return checks.stream().sorted(WORST_FIRST).toList();
    }
}
