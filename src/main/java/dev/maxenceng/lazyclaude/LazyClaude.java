package dev.maxenceng.lazyclaude;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;
import dev.maxenceng.lazyclaude.shell.ShellStartup;
import dev.maxenceng.lazyclaude.sidecar.Sidecar;

/**
 * Composition root: reads the configuration, builds the contexts and opens the frontend the arguments name, the shell or with
 * {@code --sidecar} the sidecar (ADR 0077). With {@code --protocol-schema} it builds nothing and prints the sidecar's schema.
 */
public final class LazyClaude {

    private static final String SIDECAR = "--sidecar";
    private static final String PROTOCOL_SCHEMA = "--protocol-schema";
    private static final String HERE = "";
    private static final String USER_HOME = "user.home";
    private static final String PREFIX = "lazyclaude: ";
    private static final int REFUSED = 1;

    private LazyClaude() {}

    /** A start that cannot go on prints why, before any frame or protocol line, and exits non-zero. */
    private static void refuse(String reason) {
        System.err.println(PREFIX + reason);
        System.exit(REFUSED);
    }

    /**
     * With {@code --sidecar} the protocol is the real stdout, and {@code System.out} goes to stderr before anything is built, so
     * nothing else, a library included, prints into it (ADR 0050).
     */
    static void main(String[] arguments) throws Exception {
        List<String> flags = Arrays.asList(arguments);
        if (flags.contains(PROTOCOL_SCHEMA)) {
            System.out.print(Sidecar.protocolSchema());
            return;
        }
        boolean sidecar = flags.contains(SIDECAR);
        OutputStream protocol = System.out;
        if (sidecar) {
            System.setOut(System.err);
        }
        ConfigurationLoader loader = new ConfigurationLoader(Path.of(System.getProperty(USER_HOME)), System.getenv(), new WarningRepository());
        ConfigurationView configuration;
        try {
            configuration = loader.configuration();
        } catch (DomainException e) {
            refuse(e.getMessage());
            return;
        }
        Contexts contexts = loader.contexts(configuration);
        if (sidecar) {
            System.exit(new Sidecar(contexts).run(System.in, protocol));
        }
        Optional<Reason> refused;
        try {
            refused = ShellStartup.open(contexts, configuration, Path.of(HERE).toAbsolutePath(), failure -> failure.printStackTrace(System.err));
        } catch (DomainException e) {
            refuse(e.getMessage());
            return;
        }
        refused.ifPresent(reason -> refuse(reason.value()));
    }
}
