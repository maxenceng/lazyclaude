package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketTitle;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One backlog ticket as the board shows it, with the branch its work happens on; a field the file lacks is an explicit absence, never a stand-in. */
public record Ticket(
        TicketId id,
        TicketBranch branch,
        Status status,
        TicketContext context,
        TicketType type,
        Frontmatter frontmatter,
        TicketBody body,
        BranchChanges changes) {

    private static final String CUT = "";
    private static final String TITLE_PREFIX = "# ";
    private static final Pattern FENCED_BLOCK = Pattern.compile("(?ms)^(?:```|~~~).*?(?:^(?:```|~~~)[^\\n]*$|\\z)");

    public Ticket {
        Assert.notNull("id", id);
        Assert.notNull("branch", branch);
        Assert.notNull("status", status);
        Assert.notNull("context", context);
        Assert.notNull("type", type);
        Assert.notNull("frontmatter", frontmatter);
        Assert.notNull("body", body);
        Assert.notNull("changes", changes);
    }

    /** The verbs {@code /ticket} accepts for the ticket: those of its status, and {@code design} once an in-progress branch touches the frontend. */
    public List<Verb> verbs() {
        return mayOfferDesign() && changes.touchesFrontend()
                ? Stream.concat(status.verbs().stream(), Stream.of(Verb.DESIGN)).toList()
                : status.verbs();
    }

    /** Whether the branch's changes decide what is offered: only while the ticket is in progress. */
    public boolean mayOfferDesign() {
        return status == Status.IN_PROGRESS;
    }

    public Ticket withChanges(BranchChanges changes) {
        return new Ticket(id, branch, status, context, type, frontmatter, body, changes);
    }

    /** The first `# ` line of the body outside a code fence, stripped; derived so it can never disagree with the body. */
    public TicketTitle title() {
        return switch (body) {
            case NoBody _ -> new NoTitle();
            case Body present -> titleIn(present.value());
        };
    }

    private static TicketTitle titleIn(String text) {
        // deferred: any fence marker closes a fence, not only its own, so a `~~~` inside ``` ends it — close it when a ticket is mis-titled by one.
        return FENCED_BLOCK.matcher(text).replaceAll(CUT).lines()
                .filter(line -> line.startsWith(TITLE_PREFIX))
                .map(line -> line.substring(TITLE_PREFIX.length()).strip())
                .findFirst()
                .<TicketTitle>map(heading -> heading.isEmpty() ? new NoTitle() : new Title(heading))
                .orElseGet(NoTitle::new);
    }

    public static TicketBuilder builder() {
        return new TicketBuilder();
    }

    public static final class TicketBuilder {
        private TicketId id;
        private TicketBranch branch;
        private Status status;
        private TicketContext context;
        private TicketType type;
        private Frontmatter frontmatter;
        private TicketBody body;
        private BranchChanges changes = new UnreadChanges();

        private TicketBuilder() {}

        public TicketBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public TicketBuilder branch(TicketBranch branch) {
            this.branch = branch;
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

        public TicketBuilder changes(BranchChanges changes) {
            this.changes = changes;
            return this;
        }

        public Ticket build() {
            return new Ticket(id, branch, status, context, type, frontmatter, body, changes);
        }
    }
}
