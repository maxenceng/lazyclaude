package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.domain.NoTemplate;
import dev.maxenceng.lazyclaude.board.domain.TemplateFile;
import dev.maxenceng.lazyclaude.board.domain.TicketCaptureRefusedException;
import dev.maxenceng.lazyclaude.board.domain.TicketTemplate;
import dev.maxenceng.lazyclaude.board.domain.TicketTemplatePort;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads {@code .github/ISSUE_TEMPLATE/ticket.md} under the project's root; a project without one has no template. */
public final class TicketTemplateRepository implements TicketTemplatePort {

    private static final Path TEMPLATE = Path.of(".github", "ISSUE_TEMPLATE", "ticket.md");
    private static final String UNREADABLE = "cannot read the ticket template %s: %s";

    @Override
    public TicketTemplate templateOf(Project project) {
        Path file = project.root().resolve(TEMPLATE);
        try {
            return new TemplateFile(Files.readString(file));
        } catch (NoSuchFileException absent) {
            return new NoTemplate();
        } catch (IOException cannotRead) {
            throw new TicketCaptureRefusedException(UNREADABLE.formatted(file, cannotRead.getMessage()), cannotRead);
        }
    }
}
