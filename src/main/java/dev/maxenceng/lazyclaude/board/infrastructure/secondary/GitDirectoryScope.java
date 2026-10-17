package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.file.Path;
import java.util.Set;

import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeScope;

/**
 * What in git's common directory moves a ticket's status: {@code HEAD}, {@code packed-refs}, the branches under {@code refs/heads/}
 * and what a worktree records in {@code worktrees/<name>/} of where it is and which branch it holds.
 */
final class GitDirectoryScope implements TreeScope {

    private static final String REFS = "refs";
    private static final String HEADS = "heads";
    private static final String WORKTREES = "worktrees";
    private static final String HEAD = "HEAD";
    private static final String PACKED_REFS = "packed-refs";
    private static final String LOCK_SUFFIX = ".lock";
    private static final Set<String> WORKTREE_RECORDS = Set.of(HEAD, "gitdir");
    private static final int ADMIN_DEPTH = 2;
    private static final int RECORD_DEPTH = 3;

    private final Path gitDirectory;
    private final Path refs;
    private final Path heads;
    private final Path worktrees;

    GitDirectoryScope(Path gitDirectory) {
        this.gitDirectory = gitDirectory;
        this.refs = gitDirectory.resolve(REFS);
        this.heads = refs.resolve(HEADS);
        this.worktrees = gitDirectory.resolve(WORKTREES);
    }

    @Override
    public Path root() {
        return gitDirectory;
    }

    @Override
    public boolean descends(Path directory) {
        return directory.equals(gitDirectory)
                || directory.equals(refs)
                || directory.startsWith(heads)
                || directory.equals(worktrees)
                || worktrees.equals(directory.getParent());
    }

    @Override
    public boolean counts(Path entry) {
        Path relative = gitDirectory.relativize(entry);
        String name = relative.getFileName().toString();
        if (name.endsWith(LOCK_SUFFIX)) {
            return false;
        }
        if (entry.startsWith(heads)) {
            return !entry.equals(heads);
        }
        if (entry.startsWith(worktrees)) {
            return relative.getNameCount() == ADMIN_DEPTH
                    || relative.getNameCount() == RECORD_DEPTH && WORKTREE_RECORDS.contains(name);
        }
        return relative.getNameCount() == 1 && (name.equals(HEAD) || name.equals(PACKED_REFS));
    }
}
