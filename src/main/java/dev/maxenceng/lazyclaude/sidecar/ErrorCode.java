package dev.maxenceng.lazyclaude.sidecar;

/** The faults a response's {@code error} can carry: JSON-RPC's standard codes (ADR 0050). */
enum ErrorCode {
    PARSE_ERROR(-32700),
    INVALID_REQUEST(-32600),
    METHOD_NOT_FOUND(-32601),
    INVALID_PARAMS(-32602),
    INTERNAL_ERROR(-32603);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}
