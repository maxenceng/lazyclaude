package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.added;
import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.context;
import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.removed;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.newDiffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.oldDiffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.review.published.ThreadCommentFixture.answer;
import static dev.maxenceng.lazyclaude.review.published.ThreadCommentFixture.question;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.outdated;

import java.nio.file.Path;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.published.AddedFile;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.FileStatus;
import dev.maxenceng.lazyclaude.review.published.HunkHeader;
import dev.maxenceng.lazyclaude.review.published.ModifiedFile;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.RenamedFile;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

public final class PullRequestDiffViewFixture {

    private PullRequestDiffViewFixture() {}

    public static PullRequestPayload pullRequestPayload(Path root, PullRequestNumber number) {
        return new PullRequestPayload(root, number);
    }

    public static HunkView hunkView(String header, DiffLine... lines) {
        return HunkView.builder().header(new HunkHeader(header)).lines(List.of(lines)).build();
    }

    public static DiffFileView fileView(DiffPath path, FileStatus status, HunkView... hunks) {
        return DiffFileView.builder().path(path).status(status).hunks(List.of(hunks)).build();
    }

    public static DiffFileView modifiedFileView(DiffPath path, HunkView... hunks) {
        return fileView(path, new ModifiedFile(), hunks);
    }

    public static DiffFileView changedFileView() {
        return modifiedFileView(
                diffPath(),
                hunkView(
                        "@@ -2,4 +2,4 @@ class Board {",
                        context("keep", 2, 2),
                        removed("old", 3),
                        added("new", 3),
                        context("end", 4, 4)));
    }

    public static DiffFileView addedFileView() {
        return fileView(secondDiffPath(), new AddedFile(), hunkView("@@ -0,0 +1,2 @@", added("first", 1), added("second", 2)));
    }

    public static DiffFileView renamedFileView() {
        return fileView(newDiffPath(), new RenamedFile(oldDiffPath()));
    }

    public static ReadDiffView twoFileDiffView() {
        return readDiffViewOf(changedFileView(), addedFileView());
    }

    public static ReviewThreadView openThreadView(DiffPath path, ThreadPosition position) {
        return threadView(path, position, false, true, question(), answer());
    }

    public static ReviewThreadView resolvedThreadView(DiffPath path, ThreadPosition position) {
        return threadView(path, position, true, false, question());
    }

    public static ReviewThreadView outdatedThreadView(DiffPath path) {
        return threadView(path, outdated(), false, false, question());
    }

    public static ReadDiffView readDiffViewOf(DiffFileView... files) {
        return new ReadDiffView(List.of(files));
    }

    public static LoadingDiffView loadingDiffView() {
        return new LoadingDiffView();
    }

    public static UnavailableDiffView unavailableDiffView() {
        return UnavailableDiffView.builder().reason(reason()).tooLarge(false).build();
    }

    public static UnavailableDiffView tooLargeDiffView() {
        return UnavailableDiffView.builder().reason(reason()).tooLarge(true).build();
    }

    public static LoadingThreadsView loadingThreadsView() {
        return new LoadingThreadsView();
    }

    public static UnavailableThreadsView unavailableThreadsView() {
        return new UnavailableThreadsView(reason());
    }

    public static ReadThreadsView readThreadsViewOf(ReviewThreadView... threads) {
        return new ReadThreadsView(List.of(threads));
    }

    public static ReviewThreadView threadView(
            DiffPath path, ThreadPosition position, boolean resolved, boolean open, ThreadComment... comments) {
        return ReviewThreadView.builder()
                .path(path)
                .position(position)
                .resolved(resolved)
                .open(open)
                .comments(List.of(comments))
                .build();
    }

    public static ReviewThreadView viewOf(ReviewThread thread) {
        return ReviewThreadView.from(thread);
    }
}
