package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.escapeBytes;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ptyOutputView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RepaintPayload;
import dev.maxenceng.lazyclaude.session.published.PaneHeight;
import dev.maxenceng.lazyclaude.session.published.PaneWidth;
import dev.maxenceng.lazyclaude.session.published.SessionId;

class SidecarJsonTest {

    @Test
    void writes_an_enum_constant_as_its_name_whatever_its_text() {
        assertThat(SidecarJson.tree(Weather.SUNNY).asString()).as("the schema lists names, so the writer writes names").isEqualTo("SUNNY");
    }

    @Test
    void reads_an_enum_param_by_its_name_whatever_its_text() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"weather\":\"SUNNY\"}"), WeatherPayload.class).weather())
                .as("the schema lists names, so the reader reads names")
                .isEqualTo(Weather.SUNNY);
    }

    @Test
    void reads_a_path_param_holding_a_colon_as_that_path() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"from\":\"/tmp/a:b\"}"), BoardPayload.class).from())
                .as("the schema states a path as any string, so a colon in it is not a URI scheme")
                .isEqualTo(Path.of("/tmp/a:b"));
    }

    @Test
    void reads_a_record_of_one_component_from_that_component() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"inner\":\"/work/app\",\"label\":\"x\"}"), NestedPayload.class).inner())
                .as("the writer writes it as its component and the schema states it so, so the reader reads it so")
                .isEqualTo(new BoardPayload(Path.of("/work/app")));
    }

    @Test
    void reads_the_records_of_a_list_from_their_components() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"inner\":[\"/work/app\",\"/work/other\"],\"label\":\"x\"}"), ListPayload.class).inner())
                .containsExactly(new BoardPayload(Path.of("/work/app")), new BoardPayload(Path.of("/work/other")));
    }

    @Test
    void reads_a_record_of_several_components_as_an_object_of_them() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"session\":\"s\",\"cols\":80,\"rows\":24}"), PtyResizePayload.class))
                .isEqualTo(new PtyResizePayload(new SessionId("s"), new PaneWidth(80), new PaneHeight(24)));
    }

    @Test
    void refuses_a_param_a_record_of_one_component_refuses_as_invalid_params() {
        assertThatThrownBy(() -> SidecarJson.params(SidecarJson.parse("{\"session\":\" \"}"), RepaintPayload.class))
                .isInstanceOfSatisfying(JsonRpcFaultException.class, fault -> assertThat(fault.code()).isEqualTo(ErrorCode.INVALID_PARAMS));
    }

    @Test
    void reads_bytes_from_base64() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"session\":\"s\",\"data\":\"G1tB\"}"), PtyInputPayload.class).data())
                .isEqualTo(escapeBytes());
    }

    @Test
    void writes_bytes_as_base64() {
        assertThat(SidecarJson.tree(ptyOutputView(new SessionId("s"))).path("data").asString()).isEqualTo("G1tB");
    }
}
