package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.review.domain.ClosingIssueNumber;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** One issue a pull request closes as the GraphQL answer writes it, null where the JSON lacks it. */
record ClosingIssueRecord(Integer number, RepositoryRecord repository) {

    private static final String NUMBER = "closing issue number";
    private static final String REPOSITORY = "closing issue repository";

    MissingFields missingFields() {
        MissingFields fields = new MissingFields().absent(NUMBER, number).absent(REPOSITORY, repository);
        return repository == null ? fields : fields.and(repository.missingFields(REPOSITORY));
    }

    /** Only once {@link #missingFields()} names none. */
    boolean in(RepositoryRecord pullRequestRepository) {
        return repository.nameWithOwner().equals(pullRequestRepository.nameWithOwner());
    }

    /** Only once {@link #missingFields()} names none. */
    ClosingIssueNumber toClosingIssueNumber() {
        return new ClosingIssueNumber(number);
    }
}
