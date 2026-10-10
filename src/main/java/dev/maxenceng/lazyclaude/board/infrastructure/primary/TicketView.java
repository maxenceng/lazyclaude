package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketTitle;
import dev.maxenceng.lazyclaude.board.published.TicketType;

/** One ticket as the shell reads it: the fields the board models, then the other frontmatter keys in file order, then the body. */
public record TicketView(
        TicketId id,
        TicketTitle title,
        Status status,
        TicketContext context,
        TicketType type,
        List<FrontmatterEntryView> frontmatter,
        TicketBody body) {

    static TicketView from(Ticket ticket) {
        return builder()
                .id(ticket.id())
                .title(ticket.title())
                .status(ticket.status())
                .context(ticket.context())
                .type(ticket.type())
                .frontmatter(ticket.frontmatter().entries().stream()
                        .map(FrontmatterEntryView::from)
                        .toList())
                .body(ticket.body())
                .build();
    }

    public static TicketViewBuilder builder() {
        return new TicketViewBuilder();
    }

    public static final class TicketViewBuilder {
        private TicketId id;
        private TicketTitle title;
        private Status status;
        private TicketContext context;
        private TicketType type;
        private List<FrontmatterEntryView> frontmatter;
        private TicketBody body;

        private TicketViewBuilder() {}

        public TicketViewBuilder id(TicketId id) {
            this.id = id;
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

        public TicketView build() {
            return new TicketView(id, title, status, context, type, frontmatter, body);
        }
    }
}
