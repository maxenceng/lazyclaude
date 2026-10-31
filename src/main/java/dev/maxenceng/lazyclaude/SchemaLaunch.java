package dev.maxenceng.lazyclaude;

import java.util.List;

import dev.maxenceng.lazyclaude.sidecar.Sidecar;

/** {@code --protocol-schema}: prints the sidecar's schema and builds nothing (ADR 0073). */
final class SchemaLaunch extends Launch {

    private static final String FLAG = "--protocol-schema";
    private static final int PRINTED = 0;

    static boolean askedBy(List<String> arguments) {
        return arguments.contains(FLAG);
    }

    @Override
    int run() {
        System.out.print(Sidecar.protocolSchema());
        return PRINTED;
    }
}
