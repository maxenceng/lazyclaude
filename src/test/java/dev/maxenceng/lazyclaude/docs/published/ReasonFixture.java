package dev.maxenceng.lazyclaude.docs.published;

public final class ReasonFixture {

    private ReasonFixture() {}

    public static Reason reason() {
        return new Reason("cannot be read: AccessDeniedException");
    }
}
