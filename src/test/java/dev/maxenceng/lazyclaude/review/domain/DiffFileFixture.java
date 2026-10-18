package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.added;
import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.context;
import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.removed;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.newDiffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.oldDiffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;

import java.util.List;

import dev.maxenceng.lazyclaude.review.published.AddedFile;
import dev.maxenceng.lazyclaude.review.published.DeletedFile;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.FileStatus;
import dev.maxenceng.lazyclaude.review.published.HunkHeader;
import dev.maxenceng.lazyclaude.review.published.ModifiedFile;
import dev.maxenceng.lazyclaude.review.published.RenamedFile;

public final class DiffFileFixture {

    private DiffFileFixture() {}

    public static Hunk hunk(String header, DiffLine... lines) {
        return Hunk.builder().header(new HunkHeader(header)).lines(List.of(lines)).build();
    }

    public static DiffFile file(DiffPath path, FileStatus status, Hunk... hunks) {
        return DiffFile.builder().path(path).status(status).hunks(List.of(hunks)).build();
    }

    public static DiffFile modifiedFile() {
        return file(
                diffPath(),
                new ModifiedFile(),
                hunk(
                        "@@ -2,4 +2,5 @@ class Board {",
                        context("keep", 2, 2),
                        removed("old", 3),
                        added("new", 3),
                        added("extra", 4),
                        context("end", 4, 5)));
    }

    public static DiffFile addedFile() {
        return file(secondDiffPath(), new AddedFile(), hunk("@@ -0,0 +1,2 @@", added("first", 1), added("second", 2)));
    }

    public static DiffFile deletedFile() {
        return file(secondDiffPath(), new DeletedFile(), hunk("@@ -1,2 +0,0 @@", removed("first", 1), removed("second", 2)));
    }

    public static DiffFile renamedFile() {
        return file(
                newDiffPath(),
                new RenamedFile(oldDiffPath()),
                hunk("@@ -1,2 +1,2 @@", context("keep", 1, 1), removed("old", 2), added("new", 2)));
    }

    public static DiffFile renamedFileWithoutChanges() {
        return file(newDiffPath(), new RenamedFile(oldDiffPath()));
    }

    public static DiffFile fileWithoutHunks() {
        return file(diffPath(), new ModifiedFile());
    }

    public static DiffFile fileFromTheFirstLine() {
        return file(diffPath(), new ModifiedFile(), hunk("@@ -1 +1 @@", removed("old", 1), added("new", 1)));
    }

    public static DiffFile fileWithTwoHunks() {
        return file(
                diffPath(),
                new ModifiedFile(),
                hunk("@@ -2,2 +2,2 @@", context("one", 2, 2), added("two", 3)),
                hunk("@@ -40,2 +41,2 @@", removed("three", 40), context("four", 41, 41)));
    }

    public static DiffFile fileWithBlankContext() {
        return file(
                diffPath(),
                new ModifiedFile(),
                hunk("@@ -1,3 +1,3 @@", context("a", 1, 1), context("", 2, 2), removed("-- not a header", 3), added("++ nor this", 3)));
    }

    public static PullRequestDiff pullRequestDiffOf(DiffFile... files) {
        return new PullRequestDiff(List.of(files));
    }
}
