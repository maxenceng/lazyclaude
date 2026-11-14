package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.newTicketClaude;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.ticketClaudeUnder;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.TicketPluginFixture.ticketPlugin;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TicketPlugin;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class HostLaunchFixture {

    private HostLaunchFixture() {}

    public static HostLaunch newLaunch(SessionId id) {
        return HostLaunch.builder()
                .command(new NewClaude(id))
                .directory(projectRootDirectory())
                .size(paneSize())
                .build();
    }

    public static HostLaunch newTicketLaunch(SessionId id) {
        return HostLaunch.builder()
                .command(newTicketClaude(id))
                .directory(projectRootDirectory())
                .size(paneSize())
                .build();
    }

    public static HostLaunch resumeLaunch(SessionId id, WorkingDirectory directory) {
        return HostLaunch.builder()
                .command(new ResumeClaude(id))
                .directory(directory)
                .size(paneSize())
                .build();
    }

    public static HostLaunch ticketLaunch(SessionId id, WorkingDirectory directory, Verb verb) {
        return ticketLaunchUnder(id, directory, verb, ticketPlugin());
    }

    public static HostLaunch ticketLaunchUnder(SessionId id, WorkingDirectory directory, Verb verb, TicketPlugin plugin) {
        return HostLaunch.builder()
                .command(ticketClaudeUnder(id, verb, plugin))
                .directory(directory)
                .size(paneSize())
                .build();
    }
}
