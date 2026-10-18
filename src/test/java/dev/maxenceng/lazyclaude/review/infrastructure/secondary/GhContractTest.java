package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.realDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.realThreads;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.domain.DiffFile;
import dev.maxenceng.lazyclaude.review.published.AddedFile;

@Tag("contract")
class GhContractTest {

    @Test
    void shouldParseTheDiffGhPrDiffPrintsForARealPullRequestOfThisRepository() {
        assertThat(realDiff().files())
                .as("gh pr diff --color never must still print a unified diff the parser reads")
                .isNotEmpty()
                .allSatisfy(file -> assertThat(file.path().value()).isNotBlank())
                .anySatisfy(file -> assertThat(file.status()).isInstanceOf(AddedFile.class))
                .flatExtracting(DiffFile::hunks)
                .isNotEmpty();
    }

    @Test
    void shouldParseTheThreadsTheGraphqlQueryAnswersForARealPullRequestOfThisRepository() {
        assertThat(realThreads())
                .as("the reviewThreads query must still answer the fields the parser reads")
                .isNotEmpty()
                .allSatisfy(thread -> assertThat(thread.comments()).isNotEmpty());
    }
}
