package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** What the host tells the domain when it cannot keep what it loads on its first spawn across runs, so every run leaves a copy behind. */
@FunctionalInterface
public interface HostFallback {

    /** Said once per run, on the first spawn that meets it, with the cause in the reader's words, naming the folder. */
    void leaking(Reason cause);
}
