package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unavailableDocsView;
import static dev.maxenceng.lazyclaude.sidecar.DocsSidecarFixture.docsChanged;
import static dev.maxenceng.lazyclaude.sidecar.DocsSidecarFixture.docsChangingAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.DocsSidecarFixture.docsParams;
import static dev.maxenceng.lazyclaude.sidecar.DocsSidecarFixture.docsPayload;
import static dev.maxenceng.lazyclaude.sidecar.DocsSidecarFixture.emptyDocs;
import static dev.maxenceng.lazyclaude.sidecar.DocsSidecarFixture.otherDocsParams;
import static dev.maxenceng.lazyclaude.sidecar.DocsSidecarFixture.unavailableDocs;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.failingAfterReleasing;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.invalidParamsCode;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.waitingFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;

@ExtendWith(MockitoExtension.class)
class DocsSidecarTest {

    @Mock
    private DocsQuery docs;

    @Test
    void answers_the_docs_of_the_project() {
        when(docs.docsFor(docsPayload())).thenReturn(emptyDocsView());

        assertThat(linesWritten(docs, request(1, "docs/docsFor", docsParams()))).containsExactly(ready(), resultOf(1, emptyDocs()));
    }

    @Test
    void answers_why_the_docs_cannot_be_listed_as_a_result() {
        when(docs.docsFor(docsPayload())).thenReturn(unavailableDocsView());

        assertThat(linesWritten(docs, request(1, "docs/docsFor", docsParams()))).containsExactly(ready(), resultOf(1, unavailableDocs()));
    }

    @Test
    void answers_the_current_docs_to_a_watch() {
        when(docs.docsFor(docsPayload())).thenReturn(emptyDocsView());

        assertThat(linesWritten(docs, request(1, "docs/watch", docsParams()))).containsExactly(ready(), resultOf(1, emptyDocs()));
    }

    @Test
    void pushes_the_next_docs_after_the_answer() {
        when(docs.docsFor(docsPayload())).thenReturn(emptyDocsView());
        doAnswer(docsChangingAtOnce(unavailableDocsView())).when(docs).subscribe(any(), any());

        assertThat(linesWritten(docs, request(1, "docs/watch", docsParams())))
                .as("a change pushed before the watch's answer would be overwritten by the older docs it carries")
                .containsExactly(ready(), resultOf(1, emptyDocs()), docsChanged(unavailableDocs()));
    }

    @Test
    void opens_no_second_watch_of_the_same_project() {
        when(docs.docsFor(docsPayload())).thenReturn(emptyDocsView());

        linesWritten(docs, request(1, "docs/watch", docsParams()), request(2, "docs/watch", docsParams()));

        verify(docs, times(1)).subscribe(any(), any());
    }

    @Test
    void refuses_a_watch_of_another_project_with_the_invalid_params_code() {
        when(docs.docsFor(any())).thenReturn(emptyDocsView());

        List<String> written = linesWritten(docs, request(1, "docs/watch", docsParams()), request(2, "docs/watch", otherDocsParams()));

        assertThat(written)
                .as("the one watcher holds one project, so the second root watched is refused rather than taking it over")
                .filteredOn(line -> line.contains(invalidParamsCode()))
                .hasSize(1);
        verify(docs, times(1)).subscribe(any(), any());
    }

    @Test
    void opens_the_watch_again_on_a_request_that_overlaps_one_failing_to_start() {
        CountDownLatch failing = new CountDownLatch(1);
        when(docs.docsFor(any())).thenReturn(emptyDocsView()).thenAnswer(waitingFor(failing, emptyDocsView()));
        doAnswer(failingAfterReleasing(failing)).doNothing().when(docs).subscribe(any(), any());

        linesWritten(docs, request(1, "docs/watch", docsParams()), request(2, "docs/watch", docsParams()));

        verify(docs, times(2).description("a watch that let the second request in before forgetting the failed claim leaves the docs unwatched"))
                .subscribe(any(), any());
    }
}
