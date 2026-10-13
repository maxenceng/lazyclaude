package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Set;

import dev.maxenceng.lazyclaude.review.domain.Check;
import dev.maxenceng.lazyclaude.review.published.CheckName;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

/**
 * One entry of a pull request's {@code statusCheckRollup} as {@code gh} writes it, null where the JSON lacks it. A check run has a
 * {@code name}, a {@code status} and, once complete, a {@code conclusion}; a commit status has a {@code context} and a {@code state}.
 */
record CheckRecord(String name, String status, String conclusion, String context, String state) {

    private static final String COMPLETED = "COMPLETED";
    private static final String SUCCESS = "SUCCESS";
    private static final Set<String> FAILING =
            Set.of("FAILURE", "TIMED_OUT", "CANCELLED", "ACTION_REQUIRED", "STARTUP_FAILURE", "ERROR");

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when the check has no name */
    Check toCheck() {
        return Check.builder()
                .name(new CheckName(name == null ? context : name))
                .outcome(state == null ? outcomeOfRun() : outcomeOfStatus())
                .build();
    }

    private CheckOutcome outcomeOfRun() {
        if (!COMPLETED.equals(status)) {
            return CheckOutcome.PENDING;
        }
        return FAILING.contains(conclusion) ? CheckOutcome.FAILED : CheckOutcome.PASSED;
    }

    private CheckOutcome outcomeOfStatus() {
        if (FAILING.contains(state)) {
            return CheckOutcome.FAILED;
        }
        return SUCCESS.equals(state) ? CheckOutcome.PASSED : CheckOutcome.PENDING;
    }
}
