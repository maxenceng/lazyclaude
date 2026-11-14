package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.TicketPlugin;

/**
 * {@code claude --session-id <id> -n "new ticket" "/<plugin>:ticket new <description>"}: a session begun to create a ticket. It has
 * no ticket yet, so it is an unlinked session and runs no ticket.
 */
public record NewTicketClaude(SessionId session, TicketDescription description, TicketPlugin plugin)
        implements LaunchCommand {

    private static final String SESSION_NAME = "new ticket";
    private static final LinkVerb CREATING_VERB = new LinkVerb("new");

    public NewTicketClaude {
        Assert.notNull("session", session);
        Assert.notNull("description", description);
        Assert.notNull("plugin", plugin);
    }

    @Override
    public Optional<TicketRun> launchRun() {
        return Optional.empty();
    }

    /** The {@code /ticket} verb that creates a ticket: what follows it is a description, never ticket ids. */
    public static LinkVerb creatingVerb() {
        return CREATING_VERB;
    }

    /** The name the session is given, which its row shows. */
    public SessionName sessionName() {
        return new SessionName(SESSION_NAME);
    }

    public static NewTicketClaudeBuilder builder() {
        return new NewTicketClaudeBuilder();
    }

    public static final class NewTicketClaudeBuilder {
        private SessionId session;
        private TicketDescription description;
        private TicketPlugin plugin;

        private NewTicketClaudeBuilder() {}

        public NewTicketClaudeBuilder session(SessionId session) {
            this.session = session;
            return this;
        }

        public NewTicketClaudeBuilder description(TicketDescription description) {
            this.description = description;
            return this;
        }

        public NewTicketClaudeBuilder plugin(TicketPlugin plugin) {
            this.plugin = plugin;
            return this;
        }

        public NewTicketClaude build() {
            return new NewTicketClaude(session, description, plugin);
        }
    }
}
