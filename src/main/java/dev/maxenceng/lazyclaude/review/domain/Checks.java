package dev.maxenceng.lazyclaude.review.domain;

import java.util.Comparator;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;
import dev.maxenceng.lazyclaude.review.published.CiResult;
import dev.maxenceng.lazyclaude.review.published.Failed;
import dev.maxenceng.lazyclaude.review.published.NoCiResult;
import dev.maxenceng.lazyclaude.review.published.Passed;
import dev.maxenceng.lazyclaude.review.published.Pending;

/** The checks on a pull request's head. They collapse into a CI result, failed over pending over passed, and list worst first. */
public record Checks(List<Check> checks) {

    private static final Comparator<Check> WORST_FIRST =
            Comparator.comparing(Check::outcome).thenComparing(Check::name);

    public Checks {
        Assert.notNull("checks", checks);
    }

    public CiResult result() {
        return checks.stream()
                .map(Check::outcome)
                .min(Comparator.naturalOrder())
                .<CiResult>map(Checks::resultOf)
                .orElseGet(NoCiResult::new);
    }

    /** The checks failed first, then pending, then passed, by name within each. */
    public List<Check> listed() {
        return checks.stream().sorted(WORST_FIRST).toList();
    }

    private static CiResult resultOf(CheckOutcome worst) {
        return switch (worst) {
            case FAILED -> new Failed();
            case PENDING -> new Pending();
            case PASSED -> new Passed();
        };
    }
}
