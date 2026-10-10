package dev.maxenceng.lazyclaude.docs.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a project's documents are read from. */
public interface DocsPort {

    /**
     * Every file under the project's {@code docs/}, whatever it is, in the order the tree shows them
     * ({@link DocumentPath}'s); empty when the project has no {@code docs/}.
     *
     * @throws DocsUnavailableException when {@code docs/} exists but cannot be listed
     */
    List<DocumentPath> paths(Project project);

    /** The document at {@code path}; one that cannot be read is an {@link UnreadableDocument}, never a failure. */
    Document read(Project project, DocumentPath path);
}
