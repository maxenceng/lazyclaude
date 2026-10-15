package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.file.Path;
import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.BranchChanges;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.ChangedFile;
import dev.maxenceng.lazyclaude.board.domain.ChangedFiles;
import dev.maxenceng.lazyclaude.board.domain.UnreadChanges;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads what a branch changes from {@code main} with {@code git diff --name-only main...<branch>}. A run that ends non-zero, as for a
 * branch that does not exist, reads as unread changes rather than a failure; only a git that cannot be run fails.
 */
public final class BranchChangesRepository implements BranchChangesPort {

    private static final String GIT = "git";
    private static final String MAIN = "main";

    private final GitCommand git;

    public BranchChangesRepository() {
        this(GIT);
    }

    BranchChangesRepository(String git) {
        this.git = new GitCommand(git);
    }

    /** @throws BranchCopyUnavailableException when git cannot be run */
    @Override
    public BranchChanges changesOf(Project project, TicketBranch branch) {
        GitResult diffed = git.run(project.root(), List.of("diff", "--name-only", "%s...%s".formatted(MAIN, branch.value()), "--"));
        if (!diffed.succeeded()) {
            return new UnreadChanges();
        }
        return new ChangedFiles(diffed.outputText().lines()
                .filter(line -> !line.isBlank())
                .map(line -> new ChangedFile(Path.of(line)))
                .toList());
    }
}
