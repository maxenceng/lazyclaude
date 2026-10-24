package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyPort;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.CheckedOutBranch;
import dev.maxenceng.lazyclaude.board.domain.DetachedHead;
import dev.maxenceng.lazyclaude.board.domain.Worktree;
import dev.maxenceng.lazyclaude.board.domain.WorktreeHead;
import dev.maxenceng.lazyclaude.board.domain.WorktreePath;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitCommand;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitNotRunException;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitResult;

/**
 * Reads the copies of a ticket file that are not the root checkout's, through {@code git}: {@code git worktree list --porcelain} for
 * the other checkouts, and {@code git show} for a branch that is checked out nowhere. A worktree git still lists after its directory was
 * deleted is no checkout: it holds no copy, so its branch is read instead.
 */
public final class BranchCopyRepository implements BranchCopyPort {

    private static final String WORKTREE = "worktree";
    private static final String LIST = "list";
    private static final String PORCELAIN = "--porcelain";
    private static final String REV_PARSE = "rev-parse";
    private static final String VERIFY = "--verify";
    private static final String QUIET = "--quiet";
    private static final String SHOW = "show";
    private static final String LS_TREE = "ls-tree";
    private static final String NAMES_ONLY = "--name-only";
    private static final String GIT = "git";
    private static final Path BACKLOG = Path.of("docs", "backlog");
    private static final String LOCAL_BRANCHES = "refs/heads/";
    private static final String WORKTREE_PREFIX = "worktree ";
    private static final String BRANCH_PREFIX = "branch " + LOCAL_BRANCHES;
    private static final String BARE = "bare";
    private static final Pattern BLANK_LINE = Pattern.compile("\\R\\R");
    private static final int NO_SUCH_REF = 1;

    private final GitCommand git;
    private final TicketFileReader reader = new TicketFileReader();

    public BranchCopyRepository() {
        this(GIT);
    }

    BranchCopyRepository(String git) {
        this.git = new GitCommand(git);
    }

    @Override
    public List<Worktree> worktrees(Project project) {
        GitResult listed = run(project.root(), List.of(WORKTREE, LIST, PORCELAIN));
        if (!listed.succeeded()) {
            throw new BranchCopyUnavailableException(
                    "git worktree list exited with %d%s".formatted(listed.exitValue(), listed.reason()));
        }
        return BLANK_LINE.splitAsStream(listed.outputText().strip())
                .map(block -> block.lines().toList())
                .filter(lines -> !lines.contains(BARE))
                .flatMap(BranchCopyRepository::worktree)
                .toList();
    }

    @Override
    public Optional<BacklogEntry> inWorktree(Worktree worktree, FileName file) {
        Path path = worktree.path().value().resolve(BACKLOG).resolve(file.value());
        return Files.isRegularFile(path) ? reader.read(path) : Optional.empty();
    }

    @Override
    public Optional<BacklogEntry> onBranch(Project project, TicketBranch branch, FileName file) {
        String ref = LOCAL_BRANCHES + branch.value();
        GitResult found = run(project.root(), List.of(REV_PARSE, VERIFY, QUIET, ref));
        if (found.exitValue() == NO_SUCH_REF) {
            return Optional.empty();
        }
        if (!found.succeeded()) {
            throw new BranchCopyUnavailableException(
                    "git rev-parse %s exited with %d%s".formatted(ref, found.exitValue(), found.reason()));
        }
        String path = "%s:%s".formatted(ref, BACKLOG.resolve(file.value()));
        GitResult shown = run(project.root(), List.of(SHOW, path));
        if (shown.succeeded()) {
            return reader.read(file, shown.output());
        }
        GitResult listed = run(project.root(), List.of(LS_TREE, NAMES_ONLY, ref, BACKLOG.resolve(file.value()).toString()));
        if (listed.succeeded() && listed.outputText().isBlank()) {
            return Optional.empty();
        }
        throw new BranchCopyUnavailableException(
                "git show %s exited with %d%s".formatted(path, shown.exitValue(), shown.reason()));
    }

    private static Stream<Worktree> worktree(List<String> lines) {
        return lines.stream()
                .filter(line -> line.startsWith(WORKTREE_PREFIX))
                .findFirst()
                .map(line -> Path.of(line.substring(WORKTREE_PREFIX.length())))
                .filter(Files::isDirectory)
                .map(path -> Worktree.builder().path(new WorktreePath(path)).head(head(lines)).build())
                .stream();
    }

    private static WorktreeHead head(List<String> lines) {
        return lines.stream()
                .filter(line -> line.startsWith(BRANCH_PREFIX))
                .findFirst()
                .<WorktreeHead>map(line -> new CheckedOutBranch(line.substring(BRANCH_PREFIX.length())))
                .orElseGet(DetachedHead::new);
    }

    private GitResult run(Path in, List<String> arguments) {
        try {
            return git.run(in, arguments);
        } catch (GitNotRunException e) {
            throw new BranchCopyUnavailableException(e.getMessage(), e);
        }
    }
}
