package dev.maxenceng.lazyclaude.docs.published;

public final class ReasonFixture {

    private ReasonFixture() {}

    public static Reason reason() {
        return new Reason("cannot be read: AccessDeniedException");
    }

    public static Reason watchLimitReached() {
        return new Reason(
                "cannot watch /work/app/docs (User limit of inotify watches reached); if the watch limit is reached, raise fs.inotify.max_user_watches");
    }
}
