package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Set;

import dev.maxenceng.lazyclaude.review.domain.Check;
import dev.maxenceng.lazyclaude.review.published.CheckName;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/**
 * One entry of a pull request's {@code statusCheckRollup} as {@code gh} writes it, null where the JSON lacks it. A check run has a
 * {@code name}, a {@code status} and, once complete, a {@code conclusion}; a commit status has a {@code context} and a {@code state}.
 */
record CheckRecord(String name, String status, String conclusion, String context, String state) {

    private static final String NAME = "check name";
    private static final String COMPLETED = "COMPLETED";
    private static final String SUCCESS = "SUCCESS";
    private static final Set<String> FAILING =
            Set.of("FAILURE", "TIMED_OUT", "CANCELLED", "ACTION_REQUIRED", "STARTUP_FAILURE", "ERROR");

    /** A check named neither by a run's name nor by a status's context lacks its name. */
    MissingFields missingFields() {
        return new MissingFields().blank(NAME, nameOrContext());
    }

    /** Only once {@link #missingFields()} names none. */
    Check toCheck() {
        return Check.builder()
                .name(new CheckName(nameOrContext()))
                .outcome(state == null ? outcomeOfRun() : outcomeOfStatus())
                .build();
    }

    private String nameOrContext() {
        return name == null ? context : name;
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
