package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

/** Why the launch log could not be read, did not parse or could not be written; it never leaves the adapter, which tells the domain's fallback instead. */
final class LaunchLogFailureException extends RuntimeException {

    LaunchLogFailureException(String reason) {
        super(reason);
    }

    LaunchLogFailureException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
