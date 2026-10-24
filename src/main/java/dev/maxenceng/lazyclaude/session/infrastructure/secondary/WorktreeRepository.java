package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.review.published.HeadCommit;
import dev.maxenceng.lazyclaude.session.domain.CommitId;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotRemovedException;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;
import dev.maxenceng.lazyclaude.session.published.DirtyPath;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitCommand;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitNotRunException;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitResult;

/**
 * Reads, adds and removes worktrees through {@code git}: {@code git worktree list --porcelain} for where a branch is checked out, and
 * {@code git worktree add} for a new checkout. A worktree git still lists after its directory was deleted is no place to run in, and
 * is pruned before adding one, since git refuses to add a worktree where a deleted one is still listed.
 */
public final class WorktreeRepository implements WorktreePort {

    private static final String WORKTREE = "worktree";
    private static final String LIST = "list";
    private static final String PORCELAIN = "--porcelain";
    private static final String REV_PARSE = "rev-parse";
    private static final String VERIFY = "--verify";
    private static final String QUIET = "--quiet";
    private static final String ADD = "add";
    private static final String NEW_BRANCH = "-b";
    private static final String STATUS = "status";
    private static final String ALL_UNTRACKED = "--untracked-files=all";
    private static final String REV_LIST = "rev-list";
    private static final String HEAD = "HEAD";
    private static final String NOT = "--not";
    private static final String REMOTES = "--remotes";
    private static final String REMOVE = "remove";
    private static final String END_OF_OPTIONS = "--";
    private static final String PRUNE = "prune";
    private static final String LIST_COMMAND = "git worktree list";
    private static final String REV_PARSE_COMMAND = "git rev-parse";
    private static final String STATUS_COMMAND = "git status";
    private static final String REV_LIST_COMMAND = "git rev-list";
    private static final String REMOVE_COMMAND = "git worktree remove";
    private static final String PRUNE_COMMAND = "git worktree prune";
    private static final String ADD_COMMAND = "git worktree add";
    private static final String GIT = "git";
    private static final String MAIN = "main";
    private static final String LOCAL_BRANCHES = "refs/heads/";
    private static final String WORKTREE_PREFIX = "worktree ";
    private static final String BRANCH_PREFIX = "branch " + LOCAL_BRANCHES;
    private static final Pattern BLANK_LINE = Pattern.compile("\\R\\R");
    private static final int NO_SUCH_REF = 1;
    private static final int STATUS_PREFIX_LENGTH = 3;
    private static final String NOT_PREFIX = "^";

    private final GitCommand git;

    public WorktreeRepository() {
        this(GIT);
    }

    WorktreeRepository(String git) {
        this.git = new GitCommand(git);
    }

    @Override
    public Optional<WorkingDirectory> checkedOutAt(Project project, BranchName branch) {
        GitResult listed = run(project.root(), List.of(WORKTREE, LIST, PORCELAIN));
        if (!listed.succeeded()) {
            throw new WorktreeUnavailableException(failure(LIST_COMMAND, listed));
        }
        String checkedOut = BRANCH_PREFIX + branch.value();
        return BLANK_LINE.splitAsStream(listed.outputText().strip())
                .map(block -> block.lines().toList())
                .filter(lines -> lines.contains(checkedOut))
                .flatMap(lines -> lines.stream().filter(line -> line.startsWith(WORKTREE_PREFIX)).findFirst().stream())
                .map(line -> Path.of(line.substring(WORKTREE_PREFIX.length())))
                .filter(Files::isDirectory)
                .map(WorkingDirectory::new)
                .findFirst();
    }

    @Override
    public boolean branchExists(Project project, BranchName branch) {
        GitResult found = run(project.root(), List.of(REV_PARSE, VERIFY, QUIET, LOCAL_BRANCHES + branch.value()));
        if (found.exitValue() == NO_SUCH_REF) {
            return false;
        }
        if (!found.succeeded()) {
            throw new WorktreeUnavailableException(failure(REV_PARSE_COMMAND, found));
        }
        return true;
    }

    @Override
    public void addOnBranch(Project project, BranchName branch, WorkingDirectory at) {
        add(project, List.of(WORKTREE, ADD, at.path().toString(), branch.value()));
    }

    @Override
    public void addOnNewBranch(Project project, BranchName branch, WorkingDirectory at) {
        add(project, List.of(WORKTREE, ADD, NEW_BRANCH, branch.value(), at.path().toString(), MAIN));
    }

    @Override
    public boolean isWorktreeAt(Project project, WorkingDirectory at) {
        GitResult listed = run(project.root(), List.of(WORKTREE, LIST, PORCELAIN));
        if (!listed.succeeded()) {
            throw new WorktreeUnavailableException(failure(LIST_COMMAND, listed));
        }
        return listed.outputText()
                .lines()
                .filter(line -> line.startsWith(WORKTREE_PREFIX))
                .map(line -> Path.of(line.substring(WORKTREE_PREFIX.length())))
                .filter(Files::isDirectory)
                .anyMatch(path -> sameDirectory(path, at.path()));
    }

    @Override
    public List<DirtyPath> dirtyPaths(WorkingDirectory worktree) {
        GitResult status = unreadableUnless(worktree, List.of(STATUS, PORCELAIN, ALL_UNTRACKED), STATUS_COMMAND);
        return status.outputText()
                .lines()
                .filter(line -> line.length() > STATUS_PREFIX_LENGTH)
                .map(line -> new DirtyPath(line.substring(STATUS_PREFIX_LENGTH)))
                .toList();
    }

    /** The head is negated with {@code ^}, which no option begins with, since it comes from outside and could otherwise be read as one. */
    @Override
    public List<CommitId> commitsBeyond(WorkingDirectory worktree, HeadCommit mergedHead) {
        GitResult listed = unreadableUnless(
                worktree,
                List.of(REV_LIST, HEAD, NOT_PREFIX + mergedHead.value(), NOT, REMOTES),
                REV_LIST_COMMAND);
        return listed.outputText().lines().filter(line -> !line.isBlank()).map(CommitId::new).toList();
    }

    @Override
    public void remove(Project project, WorkingDirectory worktree) {
        GitResult removed;
        try {
            removed = git.run(project.root(), List.of(WORKTREE, REMOVE, END_OF_OPTIONS, worktree.path().toString()));
        } catch (GitNotRunException e) {
            throw new WorktreeNotRemovedException(e.getMessage(), e);
        }
        if (!removed.succeeded()) {
            throw new WorktreeNotRemovedException(failure(REMOVE_COMMAND, removed));
        }
    }

    private GitResult unreadableUnless(WorkingDirectory worktree, List<String> arguments, String command) {
        GitResult result;
        try {
            result = git.run(worktree.path(), arguments);
        } catch (GitNotRunException e) {
            throw new WorktreeUnavailableException(e.getMessage(), e);
        }
        if (!result.succeeded()) {
            throw new WorktreeUnavailableException(failure(command, result));
        }
        return result;
    }

    private static boolean sameDirectory(Path listed, Path wanted) {
        try {
            return Files.isSameFile(listed, wanted);
        } catch (IOException e) {
            return false;
        }
    }

    private GitResult run(Path in, List<String> arguments) {
        try {
            return git.run(in, arguments);
        } catch (GitNotRunException e) {
            throw new WorktreeUnavailableException(e.getMessage(), e);
        }
    }

    private void add(Project project, List<String> arguments) {
        GitResult pruned = run(project.root(), List.of(WORKTREE, PRUNE));
        if (!pruned.succeeded()) {
            throw new WorktreeUnavailableException(failure(PRUNE_COMMAND, pruned));
        }
        GitResult added = run(project.root(), arguments);
        if (!added.succeeded()) {
            throw new WorktreeUnavailableException(failure(ADD_COMMAND, added));
        }
    }

    private static String failure(String command, GitResult run) {
        return run.lastLine().isEmpty()
                ? "%s exited with %d".formatted(command, run.exitValue())
                : "%s: %s".formatted(command, run.lastLine());
    }
}
