package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BacklogFixture {

    private BacklogFixture() {}

    public static Path backlogIn(Path root) throws IOException {
        return Files.createDirectories(root.resolve("docs").resolve("backlog"));
    }

    public static String ticketMarkdown() {
        return "---\nid: PROJ-1\nstatus: todo\ncontext: board\ntype: feature\n---\n\n# A board from the backlog\n\nBody.\n";
    }

    public static String secondTicketMarkdownWithStatus(String status) {
        return "---\nid: PROJ-2\nstatus: %s\n---\n\n# Plan the next slice\n".formatted(status);
    }
}
