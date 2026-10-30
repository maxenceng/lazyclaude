package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.Comparator;
import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads the issues of the GitHub repository the project's directory belongs to with {@code gh issue list}: the open ones and the closed
 * ones, in one call. Nothing is kept between reads. A run that ends non-zero is the reason the board cannot be read, in the words
 * {@code gh} used where it used any.
 */
public final class IssuesRepository implements BacklogPort {

    // deferred: only the 1000 most recent issues are asked for, so older closed ones show nowhere — close it when a project has that many.
    static final List<String> ARGUMENTS = List.of(
            "issue", "list", "--state", "all", "--limit", "1000", "--json", "number,title,body,labels,state,stateReason");

    private final GhCommand gh;

    public IssuesRepository() {
        this(new GhCommand());
    }

    IssuesRepository(GhCommand gh) {
        this.gh = gh;
    }

    @Override
    public List<BacklogEntry> entries(Project project) {
        GhResult listed = gh.run(project, ARGUMENTS);
        if (!listed.succeeded()) {
            throw GhFailure.of(listed);
        }
        return IssueRecords.entries(listed.output()).stream().sorted(Comparator.comparing(BacklogEntry::id)).toList();
    }
}
