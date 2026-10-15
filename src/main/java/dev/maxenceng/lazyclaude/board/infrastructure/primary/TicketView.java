package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketTitle;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.board.published.Verb;

/** One ticket as the shell reads it: the fields the board models, then the other frontmatter keys in file order, then the body, and the verbs {@code /ticket} accepts for it. */
public record TicketView(
        TicketId id,
        TicketBranch branch,
        TicketTitle title,
        Status status,
        TicketContext context,
        TicketType type,
        List<FrontmatterEntryView> frontmatter,
        TicketBody body,
        List<Verb> verbs) {

    static TicketView from(Ticket ticket) {
        return builder()
                .id(ticket.id())
                .branch(ticket.branch())
                .title(ticket.title())
                .status(ticket.status())
                .context(ticket.context())
                .type(ticket.type())
                .frontmatter(ticket.frontmatter().entries().stream()
                        .map(FrontmatterEntryView::from)
                        .toList())
                .body(ticket.body())
                .verbs(ticket.verbs())
                .build();
    }

    public static TicketViewBuilder builder() {
        return new TicketViewBuilder();
    }

    public static final class TicketViewBuilder {
        private TicketId id;
        private TicketBranch branch;
        private TicketTitle title;
        private Status status;
        private TicketContext context;
        private TicketType type;
        private List<FrontmatterEntryView> frontmatter;
        private TicketBody body;
        private List<Verb> verbs = List.of();

        private TicketViewBuilder() {}

        public TicketViewBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public TicketViewBuilder branch(TicketBranch branch) {
            this.branch = branch;
            return this;
        }

        public TicketViewBuilder title(TicketTitle title) {
            this.title = title;
            return this;
        }

        public TicketViewBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public TicketViewBuilder context(TicketContext context) {
            this.context = context;
            return this;
        }

        public TicketViewBuilder type(TicketType type) {
            this.type = type;
            return this;
        }

        public TicketViewBuilder frontmatter(List<FrontmatterEntryView> frontmatter) {
            this.frontmatter = frontmatter;
            return this;
        }

        public TicketViewBuilder body(TicketBody body) {
            this.body = body;
            return this;
        }

        public TicketViewBuilder verbs(List<Verb> verbs) {
            this.verbs = verbs;
            return this;
        }

        public TicketView build() {
            return new TicketView(id, branch, title, status, context, type, frontmatter, body, verbs);
        }
    }
}
