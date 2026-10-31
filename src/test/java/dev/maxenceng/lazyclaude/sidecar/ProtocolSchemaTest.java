package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.ProtocolSchemaFixture.definition;
import static dev.maxenceng.lazyclaude.sidecar.ProtocolSchemaFixture.definitionOf;
import static dev.maxenceng.lazyclaude.sidecar.ProtocolSchemaFixture.protocolSchema;
import static dev.maxenceng.lazyclaude.sidecar.ProtocolSchemaFixture.schemaOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyOutputView;

import tools.jackson.databind.JsonNode;

class ProtocolSchemaTest {

    @Test
    void names_the_kind_as_the_discriminator_of_a_sealed_view() {
        assertThat(definition("board.BoardLookupView").path("discriminator").path("propertyName").asString()).isEqualTo("kind");
        assertThat(definition("board.BoardLookupView").path("oneOf")).hasSize(2);
    }

    @Test
    void leads_a_variant_with_its_required_kind_constant() {
        assertThat(definition("board.BoardView").path("properties").propertyNames().iterator().next()).isEqualTo("kind");
        assertThat(definition("board.BoardView").path("properties").path("kind").path("const").asString()).isEqualTo("board");
        assertThat(definition("board.BoardView").path("required").get(0).asString()).isEqualTo("kind");
        assertThat(definition("board.NoProjectView").path("properties").path("kind").path("const").asString()).isEqualTo("noProject");
    }

    @Test
    void closes_every_object_so_an_unknown_field_is_not_a_valid_view() {
        assertThat(definition("board.BoardView").path("additionalProperties").asBoolean(true)).isFalse();
    }

    @Test
    void writes_a_record_of_one_component_as_that_component() {
        assertThat(definition("board.NoProjectView").path("properties").path("reason").path("type").asString()).isEqualTo("string");
        assertThat(definition("board.BoardView").path("properties").path("root").path("type").asString()).isEqualTo("string");
        assertThat(protocolSchema().path("$defs").has("board.ProjectRoot")).isFalse();
    }

    @Test
    void lists_the_names_of_an_enum() {
        assertThat(definition("board.Status").path("enum").valueStream().map(name -> name.asString()))
                .containsExactly("DRAFT", "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE");
    }

    @Test
    void states_a_payload_as_an_object_of_all_its_components() {
        assertThat(protocolSchema().at("/properties/methods/properties/board~1boardFor/properties/params/$ref").asString())
                .isEqualTo("#/$defs/board.BoardPayload");
        assertThat(definition("board.BoardPayload").path("properties").path("from").path("type").asString()).isEqualTo("string");
        assertThat(definition("board.BoardPayload").path("properties").propertyNames()).containsExactly("from");
    }

    @Test
    void states_ready_from_the_constants_the_message_is_written_with() {
        assertThat(protocolSchema().at("/properties/notifications/properties/ready/properties/params/properties/protocol/const").asInt())
                .isEqualTo(JsonRpcMessages.protocolVersion());
    }

    @Test
    void lists_the_definitions_by_name() {
        assertThat(List.copyOf(protocolSchema().path("$defs").propertyNames())).isSorted();
    }

    @Test
    void writes_the_same_text_every_time() {
        assertThat(Sidecar.protocolSchema()).isEqualTo(Sidecar.protocolSchema()).endsWith("}\n");
    }

    @Test
    void refuses_a_map() {
        assertThatThrownBy(() -> schemaOf(new ProtocolMethod<>("x/map", BoardPayload.class, MapView.class)))
                .isInstanceOf(UnschemableTypeException.class);
    }

    @Test
    void states_a_payload_component_that_is_a_record_as_the_component_it_is_read_from() {
        assertThat(definitionOf(new ProtocolMethod<>("x/nested", NestedPayload.class, BoardView.class), "sidecar.NestedPayload")
                        .path("properties").path("inner").path("type").asString())
                .as("Jackson reads a record of one component from that component")
                .isEqualTo("string");
    }

    @Test
    void states_a_payload_component_that_is_a_sealed_type_as_its_alternatives() {
        assertThat(definitionOf(new ProtocolMethod<>("x/sealed", SealedPayload.class, BoardView.class), "sidecar.SealedPayload")
                        .path("properties").path("inner").path("$ref").asString())
                .as("the reader reads a sealed type by its kind, so a Payload may hold one")
                .isEqualTo("#/$defs/board.BoardLookupView");
    }

    @Test
    void keys_each_definition_by_its_module_so_two_modules_may_own_one_name() {
        assertThat(SidecarJson.parse(schemaOf(new ProtocolMethod<>("x/twoModules", BoardPayload.class, TwoModulesView.class)))
                        .path("$defs").propertyNames())
                .contains("board.Clash", "sidecar.Clash");
    }

    @Test
    void refuses_a_type_in_no_module() {
        assertThatThrownBy(() -> schemaOf(new ProtocolMethod<>("x/unplaced", BoardPayload.class, UnplacedView.class)))
                .as("a definition is keyed by its module, and the root holds none")
                .isInstanceOf(UnschemableTypeException.class);
    }

    @Test
    void refuses_two_types_of_one_module_that_share_a_simple_name() {
        assertThatThrownBy(() -> schemaOf(new ProtocolMethod<>("x/clash", BoardPayload.class, ClashView.class)))
                .isInstanceOf(DuplicateSchemaNameException.class);
    }

    @Test
    void states_a_payload_list_of_records_as_a_list_of_the_components_they_are_read_from() {
        assertThat(definitionOf(new ProtocolMethod<>("x/list", ListPayload.class, BoardView.class), "sidecar.ListPayload")
                        .path("properties").path("inner").path("items").path("type").asString())
                .isEqualTo("string");
    }

    @Test
    void states_a_payload_component_that_is_a_record_holding_a_sealed_type() {
        assertThat(definitionOf(new ProtocolMethod<>("x/wrapped", WrappedSealedPayload.class, BoardView.class), "sidecar.WrappedSealedPayload")
                        .path("properties").path("inner").path("$ref").asString())
                .isEqualTo("#/$defs/sidecar.SealedPayload");
    }

    @Test
    void states_bytes_as_base64_text() {
        JsonNode data = definitionOf(new ProtocolMethod<>("x/bytes", BoardPayload.class, PtyOutputView.class), "session.PtyOutputView")
                .path("properties").path("data");

        assertThat(data.path("type").asString()).isEqualTo("string");
        assertThat(data.path("contentEncoding").asString()).isEqualTo("base64");
    }

    @Test
    void states_an_input_by_its_params_alone() {
        assertThat(protocolSchema().at("/properties/inputs/properties/pty~1input/properties/params/$ref").asString())
                .isEqualTo("#/$defs/session.PtyInputPayload");
        assertThat(protocolSchema().at("/properties/inputs/properties/pty~1input/properties").propertyNames()).containsExactly("params");
    }

    @Test
    void states_a_char_as_the_text_it_is_written_as() {
        assertThat(definitionOf(new ProtocolMethod<>("x/letter", BoardPayload.class, LetterView.class), "sidecar.LetterView")
                        .path("properties").path("letter").path("type").asString())
                .isEqualTo("string");
    }

    @Test
    void refuses_an_array() {
        assertThatThrownBy(() -> schemaOf(new ProtocolMethod<>("x/array", BoardPayload.class, ArrayView.class)))
                .as("the writer writes an array as its identity text")
                .isInstanceOf(UnschemableTypeException.class);
    }

    @Test
    void refuses_an_interface_its_values_are_written_by_their_own_class() {
        assertThatThrownBy(() -> schemaOf(new ProtocolMethod<>("x/text", BoardPayload.class, TextView.class)))
                .as("a record behind an interface is written as an object, not as the text the schema would state")
                .isInstanceOf(UnschemableTypeException.class);
    }

    @Test
    void refuses_a_sealed_type_with_a_variant_that_carries_no_kind() {
        assertThatThrownBy(() -> schemaOf(new ProtocolMethod<>("x/forecast", BoardPayload.class, ForecastView.class)))
                .as("a variant without a kind breaks the discriminator a generated union narrows on")
                .isInstanceOf(UnschemableTypeException.class);
    }

    @Test
    void refuses_a_record_of_one_component_that_holds_itself() {
        assertThatThrownBy(() -> schemaOf(new ProtocolMethod<>("x/node", BoardPayload.class, NodeView.class)))
                .as("it unwraps into itself without end")
                .isInstanceOf(UnschemableTypeException.class);
    }

    @Test
    void states_a_method_that_answers_nothing_as_a_null_result() {
        assertThat(SidecarJson.parse(schemaOf(new ProtocolMethod<>("x/nothing", NoParams.class, Void.class)))
                        .at("/properties/methods/properties/x~1nothing/properties/result/type")
                        .asString())
                .as("a request refreshed with no View to answer is told it was taken: null, not a made-up result")
                .isEqualTo("null");
    }
}
