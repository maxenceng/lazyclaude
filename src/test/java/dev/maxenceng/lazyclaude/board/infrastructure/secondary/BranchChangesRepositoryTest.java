package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.secondTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchAtHead;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchWithFile;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.missingGit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithBacklog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.BranchChangesUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.ChangedFile;
import dev.maxenceng.lazyclaude.board.domain.ChangedFiles;
import dev.maxenceng.lazyclaude.board.domain.UnreadChanges;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class BranchChangesRepositoryTest {

    private final BranchChangesRepository repository = new BranchChangesRepository();

    @TempDir
    Path temp;

    private Project project;
    private Path root;

    @BeforeEach
    void createRepository() throws Exception {
        root = repositoryWithBacklog(temp.resolve("app"));
        project = new Project(root);
    }

    @Test
    void shouldListTheFilesTheBranchChangesFromMain() throws Exception {
        branchWithFile(root, ticketBranch().value(), "frontend/src/App.tsx", "export {};\n");

        assertThat(repository.changesOf(project, ticketBranch()))
                .isEqualTo(new ChangedFiles(List.of(new ChangedFile(Path.of("frontend/src/App.tsx")))));
    }

    @Test
    void shouldListNoFileForABranchAtMain() throws Exception {
        branchAtHead(root, ticketBranch().value());

        assertThat(repository.changesOf(project, ticketBranch())).isEqualTo(new ChangedFiles(List.of()));
    }

    @Test
    void shouldReadNothingOfABranchThatDoesNotExist() {
        assertThat(repository.changesOf(project, secondTicketBranch()))
                .as("a ticket with no branch yet has no changes to read, which is not a failure")
                .isEqualTo(new UnreadChanges());
    }

    @Test
    void shouldFailWhenGitCannotBeRun() {
        assertThatThrownBy(() -> new BranchChangesRepository(missingGit()).changesOf(project, ticketBranch()))
                .isInstanceOf(BranchChangesUnavailableException.class);
    }
}
