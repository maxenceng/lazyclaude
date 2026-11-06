package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One ticket as the board shows it: an issue's number, title, body and labels, with the branch its work happens on and when it closed; a field the issue lacks is an explicit absence, never a stand-in. */
public record Ticket(
        TicketId id,
        TicketBranch branch,
        Status status,
        TicketContext context,
        TicketType type,
        Title title,
        List<LabelName> otherLabels,
        TicketBody body,
        BranchChanges changes,
        List<Verb> statusVerbs,
        Closure closure)
        implements BacklogEntry {

    public Ticket {
        Assert.notNull("id", id);
        Assert.notNull("branch", branch);
        Assert.notNull("status", status);
        Assert.notNull("context", context);
        Assert.notNull("type", type);
        Assert.notNull("title", title);
        Assert.notNull("otherLabels", otherLabels);
        Assert.notNull("body", body);
        Assert.notNull("changes", changes);
        Assert.notNull("statusVerbs", statusVerbs);
        Assert.notNull("closure", closure);
    }

    /** The verbs its status offers, in that order; {@code design}, where listed, only while the branch touches the frontend. */
    public List<Verb> verbs() {
        return statusVerbs.stream().filter(verb -> verb != Verb.DESIGN || changes.touchesFrontend()).toList();
    }

    /** Whether the branch's changes decide what is offered: only while its status lists {@code design}. */
    public boolean mayOfferDesign() {
        return statusVerbs.contains(Verb.DESIGN);
    }

    public Ticket withChanges(BranchChanges changes) {
        return new Ticket(id, branch, status, context, type, title, otherLabels, body, changes, statusVerbs, closure);
    }

    public Ticket withStatusVerbs(List<Verb> statusVerbs) {
        return new Ticket(id, branch, status, context, type, title, otherLabels, body, changes, statusVerbs, closure);
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
        private Title title;
        private List<LabelName> otherLabels = List.of();
        private TicketBody body;
        private BranchChanges changes = new UnreadChanges();
        private List<Verb> statusVerbs;
        private Closure closure = new NotClosed();

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

        public TicketBuilder title(Title title) {
            this.title = title;
            return this;
        }

        public TicketBuilder otherLabels(List<LabelName> otherLabels) {
            this.otherLabels = otherLabels;
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

        public TicketBuilder statusVerbs(List<Verb> statusVerbs) {
            this.statusVerbs = statusVerbs;
            return this;
        }

        public TicketBuilder closure(Closure closure) {
            this.closure = closure;
            return this;
        }

        /** A ticket not given its status's verbs offers the ones its status offers by default. */
        public Ticket build() {
            Assert.notNull("status", status);
            return new Ticket(
                    id,
                    branch,
                    status,
                    context,
                    type,
                    title,
                    otherLabels,
                    body,
                    changes,
                    Optional.ofNullable(statusVerbs).orElseGet(() -> OfferedVerbs.byDefault().of(status)),
                    closure);
        }
    }
}
