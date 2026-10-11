package dev.maxenceng.lazyclaude.session.published;

public final class ReasonFixture {

    private ReasonFixture() {}

    public static Reason reason() {
        return new Reason("no registry and no claude");
    }

    public static Reason registryUnwatchable() {
        return new Reason("cannot watch /home/max/.claude/sessions (User limit of inotify watches reached)");
    }

    public static Reason registryUnreadable() {
        return new Reason("cannot read /home/max/.claude/sessions");
    }

    public static Reason transcriptsUnwatchable() {
        return new Reason(
                "cannot watch /home/max/.claude/projects (User limit of inotify watches reached); if the watch limit is reached, raise fs.inotify.max_user_watches");
    }

    public static Reason cacheUnwritable() {
        return new Reason("cannot write the cache /home/max/.cache/lazyclaude/past-sessions.json: Permission denied");
    }
}
