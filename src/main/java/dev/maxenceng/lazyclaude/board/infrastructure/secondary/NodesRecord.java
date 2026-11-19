package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.List;

/** A GraphQL connection as {@code gh api graphql} writes it: its elements under {@code nodes}, null where the JSON lacks them. */
record NodesRecord<T>(List<T> nodes) {

    /** The elements in the order GitHub lists them; none when the connection or its {@code nodes} is absent. */
    List<T> listed() {
        return nodes == null ? List.of() : nodes;
    }
}
