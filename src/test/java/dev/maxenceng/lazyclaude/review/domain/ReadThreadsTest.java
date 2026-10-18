package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.outdatedThreadIn;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.threadAt;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReadThreadsTest {

    @Test
    void shouldListThreadsByFileThenByLineWithOutdatedOnesLast() {
        ReviewThread outdatedInFirst = outdatedThreadIn(diffPath());
        ReviewThread lateInSecond = threadAt(secondDiffPath(), rightOn(9));
        ReviewThread earlyInSecond = threadAt(secondDiffPath(), rightOn(3));
        ReviewThread inFirst = threadAt(diffPath(), rightOn(5));

        assertThat(new ReadThreads(List.of(outdatedInFirst, lateInSecond, earlyInSecond, inFirst)).inDiffOrder())
                .containsExactly(inFirst, earlyInSecond, lateInSecond, outdatedInFirst);
    }

    @Test
    void shouldPlaceARangeAtTheLineItShowsUnder() {
        ReviewThread range = threadAt(diffPath(), rightRange(2, 12));
        ReviewThread single = threadAt(diffPath(), rightOn(8));

        assertThat(new ReadThreads(List.of(range, single)).inDiffOrder()).containsExactly(single, range);
    }

    @Test
    void shouldOrderOutdatedThreadsByFile() {
        ReviewThread inSecond = outdatedThreadIn(secondDiffPath());
        ReviewThread inFirst = outdatedThreadIn(diffPath());

        assertThat(new ReadThreads(List.of(inSecond, inFirst)).inDiffOrder()).containsExactly(inFirst, inSecond);
    }
}
