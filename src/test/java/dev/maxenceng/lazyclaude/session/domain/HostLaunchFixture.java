package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class HostLaunchFixture {

    private HostLaunchFixture() {}

    public static HostLaunch newLaunch(HostedSessionId id) {
        return HostLaunch.builder()
                .id(id)
                .command(new NewClaude())
                .directory(projectRootDirectory())
                .size(paneSize())
                .build();
    }

    public static HostLaunch resumeLaunch(HostedSessionId id, WorkingDirectory directory) {
        return HostLaunch.builder()
                .id(id)
                .command(new ResumeClaude(sessionId()))
                .directory(directory)
                .size(paneSize())
                .build();
    }
}
