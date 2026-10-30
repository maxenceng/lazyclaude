package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.TicketCapturePort;
import dev.maxenceng.lazyclaude.board.domain.TicketCaptureRefusedException;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Creates an issue on the GitHub repository the project's directory belongs to with {@code gh issue create}, with no labels, and
 * reads the number it was given from the address {@code gh} prints. A run that ends non-zero is the reason it was refused, in the
 * words {@code gh} used where it used any. Nothing is retried.
 */
public final class TicketCaptureRepository implements TicketCapturePort {

    private static final Pattern ISSUE_ADDRESS = Pattern.compile("/issues/(\\d+)\\s*\\z");
    private static final String NO_NUMBER = "gh issue create printed no issue number: %s";
    private static final String NO_BODY = "";
    private static final List<String> CREATE = List.of("issue", "create");
    private static final String TITLE = "--title";
    private static final String BODY = "--body";

    private final GhCommand gh;

    public TicketCaptureRepository() {
        this(new GhCommand());
    }

    TicketCaptureRepository(GhCommand gh) {
        this.gh = gh;
    }

    @Override
    public TicketId create(Project project, Title title, TicketBody body) {
        GhResult created = run(project, Stream.concat(CREATE.stream(), Stream.of(TITLE, title.value(), BODY, text(body))).toList());
        if (!created.succeeded()) {
            throw GhFailure.refusing(created);
        }
        return numbered(created.outputText());
    }

    private GhResult run(Project project, List<String> arguments) {
        try {
            return gh.run(project, arguments);
        } catch (BacklogUnavailableException cannotRun) {
            throw new TicketCaptureRefusedException(cannotRun.getMessage(), cannotRun);
        }
    }

    private static String text(TicketBody body) {
        return switch (body) {
            case Body written -> written.value();
            case NoBody _ -> NO_BODY;
        };
    }

    private static TicketId numbered(String printed) {
        Matcher address = ISSUE_ADDRESS.matcher(printed);
        if (!address.find()) {
            throw new TicketCaptureRefusedException(NO_NUMBER.formatted(printed.strip()));
        }
        return new TicketId(address.group(1));
    }
}
