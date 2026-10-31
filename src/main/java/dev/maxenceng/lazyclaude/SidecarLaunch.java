package dev.maxenceng.lazyclaude;

import java.io.OutputStream;
import java.util.List;

import dev.maxenceng.lazyclaude.sidecar.Sidecar;

/**
 * {@code --sidecar}: answers the desktop frontend on stdin and the real stdout until stdin closes (ADR 0050). {@code System.out} goes
 * to stderr before anything is built, so nothing else, a library included, prints into the protocol.
 */
final class SidecarLaunch extends Launch {

    private static final String FLAG = "--sidecar";

    static boolean askedBy(List<String> arguments) {
        return arguments.contains(FLAG);
    }

    @Override
    int run() throws Exception {
        OutputStream protocol = System.out;
        System.setOut(System.err);
        return configured((configuration, contexts) -> new Sidecar(contexts).run(System.in, protocol));
    }
}
