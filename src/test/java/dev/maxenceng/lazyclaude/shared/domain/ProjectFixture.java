package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;

public final class ProjectFixture {

    private ProjectFixture() {}

    public static Project project() {
        return new Project(Path.of("/work/app"));
    }

    public static Project anotherProject() {
        return new Project(Path.of("/work/another"));
    }

    public static Project otherProject() {
        return new Project(Path.of("/work/other"));
    }
}
