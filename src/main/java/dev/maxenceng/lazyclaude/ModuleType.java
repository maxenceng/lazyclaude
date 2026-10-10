package dev.maxenceng.lazyclaude;

/** What a direct subpackage of the root is, which decides what it may reach and what may reach it. */
public enum ModuleType {
    /** A bounded context: reached by other contexts only through its {@code published} package. */
    CONTEXT,

    /** A shared kernel: plain types every module may use; it reaches no module itself. */
    KERNEL,

    /** The terminal application: asks a context through its {@code infrastructure.primary} and reads its {@code published} package. */
    SHELL
}
