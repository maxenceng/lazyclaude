package dev.maxenceng.lazyclaude.sidecar;

/** A line or a call that cannot be answered with a result; it travels as the response's {@code error}. */
final class JsonRpcFaultException extends RuntimeException {

    private final transient ErrorCode code;

    JsonRpcFaultException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    JsonRpcFaultException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    ErrorCode code() {
        return code;
    }
}
