package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.Arrays;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.domain.Frontmatter;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.board.published.TypeName;

/** The four frontmatter fields the board models, as text (null where the file lacks one), and the file's other keys. */
record TicketFrontmatter(String id, String status, String context, String type, Frontmatter otherKeys) {

    private static final String NO_STATUS = "status is mandatory and was not set";
    private static final String NOT_AN_ID = "id '%s' is not %s";
    private static final String NOT_A_STATUS = "status '%s' is not one of %s";
    private static final String LABEL_SEPARATOR = ", ";

    boolean claimsATicket() {
        return id != null;
    }

    /** Why the board cannot take the ticket, read before anything is built from it: no status, or an id or a status the board has no word for. */
    Optional<String> refusal() {
        if (status == null) {
            return Optional.of(NO_STATUS);
        }
        if (!TicketId.fits(id)) {
            return Optional.of(NOT_AN_ID.formatted(id, TicketId.shape()));
        }
        return Status.find(status).isPresent() ? Optional.empty() : Optional.of(NOT_A_STATUS.formatted(status, labels()));
    }

    /** Only once {@link #refusal()} is empty. */
    Ticket toDomain(TicketBranch branch, TicketBody body) {
        return Ticket.builder()
                .id(new TicketId(id))
                .branch(branch)
                .status(Status.find(status).orElseThrow())
                .context(ticketContext())
                .type(ticketType())
                .frontmatter(otherKeys)
                .body(body)
                .build();
    }

    private static String labels() {
        return String.join(LABEL_SEPARATOR, Arrays.stream(Status.values()).map(Status::label).toList());
    }

    private TicketContext ticketContext() {
        return isAbsent(context) ? new NoContext() : new ContextName(context);
    }

    private TicketType ticketType() {
        return isAbsent(type) ? new NoType() : new TypeName(type);
    }

    private static boolean isAbsent(String field) {
        return field == null || field.isBlank();
    }
}
