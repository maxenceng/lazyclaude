package dev.maxenceng.lazyclaude;

import static dev.maxenceng.lazyclaude.SpawnedPtyHost.extractedLibraryIn;
import static dev.maxenceng.lazyclaude.SpawnedPtyHost.pty4jFoldersIn;
import static dev.maxenceng.lazyclaude.SpawnedPtyHost.started;
import static dev.maxenceng.lazyclaude.SpawnedPtyHost.tmpdirOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PtyNativesTest {

    @TempDir
    Path root;

    @Test
    void shouldExtractIntoTheCacheAndLeaveNothingInTheTempFolder() throws Exception {
        Path cache = Files.createDirectory(root.resolve("cache"));
        try (SpawnedPtyHost child = started(root, "only", cache)) {
            child.awaitReady();
            child.go();

            assertThat(child.awaitExit()).as(child.stderr()).isZero();
        }

        assertThat(pty4jFoldersIn(tmpdirOf(root, "only")))
                .as("pty4j's deleteOnExit cannot remove a folder holding its natives")
                .isEmpty();
        assertThat(extractedLibraryIn(cache)).as("later runs reuse the natives extracted here").isRegularFile();
    }

    @RepeatedTest(5)
    void shouldStartBothOfTwoProcessesExtractingAtOnce() throws Exception {
        Path cache = Files.createDirectory(root.resolve("cache"));
        try (SpawnedPtyHost first = started(root, "first", cache);
                SpawnedPtyHost second = started(root, "second", cache)) {
            first.awaitReady();
            second.awaitReady();
            first.go();
            second.go();

            assertThat(first.awaitExit())
                    .as("without the lock one in two such pairs fails to extract: %s", first.stderr())
                    .isZero();
            assertThat(second.awaitExit()).as(second.stderr()).isZero();
        }

        assertThat(extractedLibraryIn(cache)).isRegularFile();
    }
}
