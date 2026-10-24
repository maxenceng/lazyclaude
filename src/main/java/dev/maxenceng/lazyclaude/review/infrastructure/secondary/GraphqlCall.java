package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.stream.Stream;

/**
 * The arguments of a {@code gh api graphql} call, in the order they are added: a variable is given as {@code -f name=value} when it is
 * text and as {@code -F name=value} when {@code gh} is to read it as a number or fill it in, as {@code {owner}} is.
 */
record GraphqlCall(List<String> arguments) {

    private static final String API = "api";
    private static final String GRAPHQL = "graphql";
    private static final String PAGINATE = "--paginate";
    private static final String TEXT = "-f";
    private static final String TYPED = "-F";
    private static final String JQ = "--jq";
    private static final String QUERY = "query";
    private static final String ASSIGNED = "%s=%s";

    GraphqlCall() {
        this(List.of(API, GRAPHQL));
    }

    /** Asks {@code gh} to page through every page of the answer. */
    GraphqlCall paginated() {
        return with(PAGINATE);
    }

    GraphqlCall query(String query) {
        return text(QUERY, query);
    }

    GraphqlCall text(String name, String value) {
        return with(TEXT, ASSIGNED.formatted(name, value));
    }

    GraphqlCall typed(String name, Object value) {
        return with(TYPED, ASSIGNED.formatted(name, value));
    }

    /** Has {@code gh} print only what {@code filter} picks out of the answer. */
    GraphqlCall picking(String filter) {
        return with(JQ, filter);
    }

    private GraphqlCall with(String... added) {
        return new GraphqlCall(Stream.concat(arguments.stream(), Stream.of(added)).toList());
    }
}
