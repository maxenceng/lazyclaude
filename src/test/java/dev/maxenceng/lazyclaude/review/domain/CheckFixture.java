package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.CheckNameFixture.analyse;
import static dev.maxenceng.lazyclaude.review.published.CheckNameFixture.build;
import static dev.maxenceng.lazyclaude.review.published.CheckNameFixture.contract;
import static dev.maxenceng.lazyclaude.review.published.CheckNameFixture.lint;

import java.util.List;

import dev.maxenceng.lazyclaude.review.published.CheckName;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

public final class CheckFixture {

    private CheckFixture() {}

    public static Check check(CheckName name, CheckOutcome outcome) {
        return Check.builder().name(name).outcome(outcome).build();
    }

    public static Check passedLint() {
        return check(lint(), CheckOutcome.PASSED);
    }

    public static Check pendingContract() {
        return check(contract(), CheckOutcome.PENDING);
    }

    public static Check failedAnalyse() {
        return check(analyse(), CheckOutcome.FAILED);
    }

    public static Check failedBuild() {
        return check(build(), CheckOutcome.FAILED);
    }

    public static Checks checksOf(Check... checks) {
        return new Checks(List.of(checks));
    }

    public static Checks noChecks() {
        return new Checks(List.of());
    }
}
