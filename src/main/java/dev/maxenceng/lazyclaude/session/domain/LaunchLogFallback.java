package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** What the launch log's adapter tells the domain when the log cannot be read, does not parse or cannot be written: said once, then never again. */
@FunctionalInterface
public interface LaunchLogFallback {

    /** Said on the first failure only, with the cause in the reader's words, naming the file and line when the log did not parse. */
    void unavailable(Reason cause);
}
