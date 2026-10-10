package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketTitle;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One backlog ticket as the board shows it; a field the file lacks is an explicit absence, never a stand-in. */
public record Ticket(
        TicketId id,
        Status status,
        TicketContext context,
        TicketType type,
        Frontmatter frontmatter,
        TicketBody body) {

    private static final String TITLE_PREFIX = "# ";
    private static final List<String> CODE_FENCES = List.of("```", "~~~");

    public Ticket {
        Assert.notNull("id", id);
        Assert.notNull("status", status);
        Assert.notNull("context", context);
        Assert.notNull("type", type);
        Assert.notNull("frontmatter", frontmatter);
        Assert.notNull("body", body);
    }

    /** The first `# ` line of the body outside a code fence, stripped; derived so it can never disagree with the body. */
    public TicketTitle title() {
        return switch (body) {
            case NoBody noBody -> new NoTitle();
            case Body present -> titleIn(present.value());
        };
    }

    private static TicketTitle titleIn(String text) {
        boolean inCode = false;
        // deferred: any fence marker closes a fence, not only its own, so a `~~~` inside ``` ends it — close it when a ticket is mis-titled by one.
        for (String line : text.lines().toList()) {
            if (CODE_FENCES.stream().anyMatch(line::startsWith)) {
                inCode = !inCode;
            } else if (!inCode && line.startsWith(TITLE_PREFIX)) {
                String heading = line.substring(TITLE_PREFIX.length()).strip();
                return heading.isEmpty() ? new NoTitle() : new Title(heading);
            }
        }
        return new NoTitle();
    }

    public static TicketBuilder builder() {
        return new TicketBuilder();
    }

    public static final class TicketBuilder {
        private TicketId id;
        private Status status;
        private TicketContext context;
        private TicketType type;
        private Frontmatter frontmatter;
        private TicketBody body;

        private TicketBuilder() {}

        public TicketBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public TicketBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public TicketBuilder context(TicketContext context) {
            this.context = context;
            return this;
        }

        public TicketBuilder type(TicketType type) {
            this.type = type;
            return this;
        }

        public TicketBuilder frontmatter(Frontmatter frontmatter) {
            this.frontmatter = frontmatter;
            return this;
        }

        public TicketBuilder body(TicketBody body) {
            this.body = body;
            return this;
        }

        public Ticket build() {
            return new Ticket(id, status, context, type, frontmatter, body);
        }
    }
}
