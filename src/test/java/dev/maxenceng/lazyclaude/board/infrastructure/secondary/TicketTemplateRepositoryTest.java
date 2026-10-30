package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateWithFrontMatter;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.noTemplate;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.projectAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.TemplateFile;
import dev.maxenceng.lazyclaude.board.domain.TicketCaptureRefusedException;

class TicketTemplateRepositoryTest {

    @TempDir
    Path root;

    @Test
    void shouldReadTheTemplateUnderTheProjectsRoot() throws IOException {
        Path directory = Files.createDirectories(root.resolve(".github/ISSUE_TEMPLATE"));
        Files.writeString(directory.resolve("ticket.md"), ((TemplateFile) templateWithFrontMatter()).text());

        assertThat(new TicketTemplateRepository().templateOf(projectAt(root))).isEqualTo(templateWithFrontMatter());
    }

    @Test
    void shouldHaveNoTemplateWhenTheProjectHasNone() {
        assertThat(new TicketTemplateRepository().templateOf(projectAt(root))).isEqualTo(noTemplate());
    }

    @Test
    void shouldRefuseATemplateThatCannotBeRead() throws IOException {
        Files.createDirectories(root.resolve(".github/ISSUE_TEMPLATE/ticket.md"));

        assertThatThrownBy(() -> new TicketTemplateRepository().templateOf(projectAt(root)))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessageStartingWith("cannot read the ticket template ");
    }
}
