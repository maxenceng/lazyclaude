package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

class TicketFrontmatterMapperTest {

    private final TicketFrontmatterMapper mapper = new TicketFrontmatterMapper();

    @Test
    void shouldShowADateAsWritten() {
        assertThat(mapper.read("created: 2026-10-10").entries().entries()).containsExactly(entry("created", "2026-10-10"));
    }

    @Test
    void shouldShowANumberAsWritten() {
        assertThat(mapper.read("tracker_id: 073\nratio: 1.50\nhex: 0x1F").entries().entries())
                .as("the source text, not the number it resolves to")
                .containsExactly(entry("tracker_id", "073"), entry("ratio", "1.50"), entry("hex", "0x1F"));
    }

    @Test
    void shouldShowAListAsBracketedItems() {
        assertThat(mapper.read("blocked_by:\n  - PROJ-8\n  - PROJ-9").entries().entries())
                .containsExactly(entry("blocked_by", "[PROJ-8, PROJ-9]"));
    }

    @Test
    void shouldShowAnEmptyListAsEmptyBrackets() {
        assertThat(mapper.read("blocked_by: []").entries().entries()).containsExactly(entry("blocked_by", "[]"));
    }

    @Test
    void shouldShowANestedMapAsBracedPairs() {
        assertThat(mapper.read("links: {a: 1, b: two}").entries().entries()).containsExactly(entry("links", "{a: 1, b: two}"));
    }

    @Test
    void shouldShowAKeyWithNothingAfterItAsEmpty() {
        assertThat(mapper.read("parent:").entries().entries()).containsExactly(entry("parent", ""));
    }

    @Test
    void shouldShowAQuotedValueWithoutItsQuotes() {
        assertThat(mapper.read("note: 'a: b'").entries().entries()).containsExactly(entry("note", "a: b"));
    }

    @Test
    void shouldKeepKeysInFileOrder() {
        assertThat(mapper.read("zeta: 1\nalpha: 2\nmid: 3").entries().entries())
                .containsExactly(entry("zeta", "1"), entry("alpha", "2"), entry("mid", "3"));
    }

    @Test
    void shouldReadTheFieldsTheBoardModels() {
        TicketFrontmatter read = mapper.read("id: PROJ-1\nstatus: todo\ncontext: board\ntype: feature");

        assertThat(read.id()).isEqualTo("PROJ-1");
        assertThat(read.status()).isEqualTo("todo");
        assertThat(read.context()).isEqualTo("board");
        assertThat(read.type()).isEqualTo("feature");
    }

    @Test
    void shouldReadAKeyWithNothingAfterItAsAbsentField() {
        assertThat(mapper.read("id:\nstatus: null").id()).as("an empty id is no id").isNull();
        assertThat(mapper.read("id:\nstatus: null").status()).isNull();
    }

    @Test
    void shouldReadNothingFromFrontmatterThatIsNotAMap() {
        TicketFrontmatter read = mapper.read("- just\n- a list");

        assertThat(read.claimsATicket()).isFalse();
        assertThat(read.entries().entries()).isEmpty();
    }

    @Test
    void shouldReadNothingFromEmptyFrontmatter() {
        assertThat(mapper.read("").entries().entries()).isEmpty();
    }

    @Test
    void shouldRefuseFrontmatterThatIsNotYaml() {
        assertThatThrownBy(() -> mapper.read("status: [todo")).isInstanceOf(YamlEngineException.class);
    }

    @Test
    void shouldRefuseAKeyWrittenTwice() {
        assertThatThrownBy(() -> mapper.read("id: PROJ-1\nstatus: todo\nstatus: done"))
                .as("which of the two values is meant cannot be told, so neither is taken")
                .isInstanceOf(YamlEngineException.class);
    }

    @Test
    void shouldRefuseAValueThatContainsItself() {
        assertThatThrownBy(() -> mapper.read("id: PROJ-1\nloop: &a [*a]")).isInstanceOf(YamlEngineException.class);
    }

    @Test
    void shouldShowAValueWrittenOverSeveralLinesOnOneRow() {
        assertThat(mapper.read("notes: |\n  first line\n\n  second line\n").entries().entries())
                .as("a main view row holds one line, so a line break would draw control characters into it")
                .containsExactly(entry("notes", "first line second line"));
    }

    @Test
    void shouldKeepTheSpacesOfAOneLineValue() {
        assertThat(mapper.read("note: ' padded '").entries().entries()).containsExactly(entry("note", " padded "));
    }
}
