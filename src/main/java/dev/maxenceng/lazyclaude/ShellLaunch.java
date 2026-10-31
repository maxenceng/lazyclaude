package dev.maxenceng.lazyclaude;

import java.nio.file.Path;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.shell.ShellStartup;

/** Without a flag: the TUI on the project of the working directory, until the user quits. */
final class ShellLaunch extends Launch {

    private static final String HERE = "";
    private static final int QUIT = 0;

    @Override
    int run() throws Exception {
        return configured((configuration, contexts) -> {
            Optional<Reason> refusal;
            try {
                refusal = ShellStartup.open(contexts, configuration, Path.of(HERE).toAbsolutePath(), failure -> failure.printStackTrace(System.err));
            } catch (DomainException e) {
                return refused(e.getMessage());
            }
            return refusal.map(reason -> refused(reason.value())).orElse(QUIT);
        });
    }
}
