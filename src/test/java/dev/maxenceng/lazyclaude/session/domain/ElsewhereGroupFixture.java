package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.anotherProject;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.otherProject;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.outsideGitDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.secondOutsideGitDirectory;

import java.util.List;

public final class ElsewhereGroupFixture {

    private ElsewhereGroupFixture() {}

    public static ElsewhereGroup otherProjectGroup(List<LiveSession> sessions) {
        return new ElsewhereGroup(new ProjectPlace(otherProject()), sessions);
    }

    public static ElsewhereGroup anotherProjectGroup(List<LiveSession> sessions) {
        return new ElsewhereGroup(new ProjectPlace(anotherProject()), sessions);
    }

    public static ElsewhereGroup secondOutsideGitGroup(List<LiveSession> sessions) {
        return new ElsewhereGroup(new OutsideGitPlace(secondOutsideGitDirectory()), sessions);
    }

    public static ElsewhereGroup outsideGitGroup(List<LiveSession> sessions) {
        return new ElsewhereGroup(new OutsideGitPlace(outsideGitDirectory()), sessions);
    }
}
