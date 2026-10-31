package dev.maxenceng.lazyclaude;

import java.nio.file.Path;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;

/**
 * How lazyclaude is launched, picked from its arguments by {@link #from}: the composition root's one choice, and with
 * {@code LazyClaude} the only place that prints, before any frame or protocol line (ADR 0077).
 */
abstract sealed class Launch permits SchemaLaunch, SidecarLaunch, ShellLaunch {

    private static final String PREFIX = "lazyclaude: ";
    private static final String USER_HOME = "user.home";
    private static final int REFUSED = 1;

    static Launch from(List<String> arguments) {
        if (SchemaLaunch.askedBy(arguments)) {
            return new SchemaLaunch();
        }
        if (SidecarLaunch.askedBy(arguments)) {
            return new SidecarLaunch();
        }
        return new ShellLaunch();
    }

    /** Runs until the frontend ends, and returns the exit code. */
    abstract int run() throws Exception;

    /** Runs {@code launch} on what the user configured and every context built over it, or prints why the configuration is refused. */
    static int configured(ConfiguredLaunch launch) throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader(Path.of(System.getProperty(USER_HOME)), System.getenv());
        ConfigurationView configuration;
        try {
            configuration = loader.configuration();
        } catch (DomainException e) {
            return refused(e.getMessage());
        }
        return launch.run(configuration, loader.contexts(configuration));
    }

    /** A start that cannot go on prints why and exits non-zero. */
    static int refused(String reason) {
        System.err.println(PREFIX + reason);
        return REFUSED;
    }
}
