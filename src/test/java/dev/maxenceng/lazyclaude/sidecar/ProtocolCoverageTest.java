package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.ProtocolCoverageFixture.notYetOnTheProtocol;
import static dev.maxenceng.lazyclaude.sidecar.ProtocolCoverageFixture.operations;
import static dev.maxenceng.lazyclaude.sidecar.ProtocolCoverageFixture.protocolMethods;
import static dev.maxenceng.lazyclaude.sidecar.ProtocolCoverageFixture.servedByAWatch;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ProtocolCoverageTest {

    @Test
    void every_public_query_and_command_operation_has_a_protocol_method_or_is_listed_as_not_yet_on_it() {
        List<String> unaccounted = operations().stream()
                .filter(operation -> !protocolMethods().contains(operation))
                .filter(operation -> !servedByAWatch().contains(operation))
                .filter(operation -> !notYetOnTheProtocol().contains(operation))
                .toList();

        assertThat(unaccounted)
                .as("these operations have no protocol method: add one in its context's methods class, or list it as not yet on the protocol")
                .isEmpty();
    }

    @Test
    void an_operation_listed_as_not_yet_on_the_protocol_is_not_a_protocol_method() {
        List<String> listedAndServed = Stream.concat(notYetOnTheProtocol().stream(), servedByAWatch().stream())
                .filter(protocolMethods()::contains)
                .toList();

        assertThat(listedAndServed).as("these operations have a method now: take them off the list so it only shrinks").isEmpty();
    }

    @Test
    void every_operation_listed_as_not_yet_on_the_protocol_still_exists() {
        assertThat(operations()).as("an operation that was removed or renamed goes off the list").containsAll(notYetOnTheProtocol()).containsAll(servedByAWatch());
    }

    @Test
    void no_two_operations_of_one_context_share_a_name() {
        assertThat(operations()).as("one protocol method name would stand for both: rename one").doesNotHaveDuplicates();
    }
}
