package dev.maxenceng.lazyclaude.session.published;

public final class ProcessIdFixture {

    private ProcessIdFixture() {}

    public static ProcessId processId() {
        return new ProcessId(17234);
    }

    public static ProcessId secondProcessId() {
        return new ProcessId(22862);
    }

    public static ProcessId thirdProcessId() {
        return new ProcessId(1761);
    }

    public static ProcessId fourthProcessId() {
        return new ProcessId(40117);
    }
}
