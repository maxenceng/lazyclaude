package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** A repository as the GraphQL answer names it, by its owner and name, null where the JSON lacks it. */
record RepositoryRecord(String nameWithOwner) {

    MissingFields missingFields(String field) {
        return new MissingFields().blank(field, nameWithOwner);
    }
}
