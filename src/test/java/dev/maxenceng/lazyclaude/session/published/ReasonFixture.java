package dev.maxenceng.lazyclaude.session.published;

public final class ReasonFixture {

    private ReasonFixture() {}

    public static Reason reason() {
        return new Reason("no registry and no claude");
    }
}
