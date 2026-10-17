package dev.maxenceng.lazyclaude.board.published;

public final class ReasonFixture {

    private ReasonFixture() {}

    public static Reason reason() {
        return new Reason("status is missing");
    }

    public static Reason treeUnwatchable() {
        return new Reason(
                "cannot watch /home/max/projects/app/docs/backlog (User limit of inotify watches reached; if the watch limit is reached, raise fs.inotify.max_user_watches)");
    }

    public static Reason backlogMissing() {
        return new Reason("the main worktree has no /x/docs/backlog");
    }
}
