package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.TicketCaptureFixture.captureRefused;
import static dev.maxenceng.lazyclaude.board.domain.TicketCaptureFixture.capturedTicket;
import static dev.maxenceng.lazyclaude.board.domain.TicketCaptureFixture.refusedCapture;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.noTemplate;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateSections;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateWithFrontMatter;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;

@ExtendWith(MockitoExtension.class)
class TicketCaptureManagerTest {

    @Mock
    private TicketCapturePort captures;

    @Mock
    private TicketTemplatePort templates;

    private TicketCaptureManager manager;

    @BeforeEach
    void createManager() {
        manager = new TicketCaptureManager(captures, templates);
    }

    @Test
    void shouldCreateTheTicketWithTheTemplatesSectionsAndAnswerItsNumber() {
        when(templates.templateOf(project())).thenReturn(templateWithFrontMatter());
        when(captures.create(project(), title(), new Body(templateSections()))).thenReturn(ticketId());

        assertThat(manager.capture(project(), title())).isEqualTo(capturedTicket());
    }

    @Test
    void shouldCreateTheTicketEmptyWhenTheProjectHasNoTemplate() {
        when(templates.templateOf(project())).thenReturn(noTemplate());
        when(captures.create(project(), title(), new NoBody())).thenReturn(ticketId());

        assertThat(manager.capture(project(), title())).isEqualTo(capturedTicket());
    }

    @Test
    void shouldAnswerWhyGitHubRefusedInsteadOfFailing() {
        when(templates.templateOf(project())).thenReturn(noTemplate());
        when(captures.create(any(), any(), any())).thenThrow(captureRefused());

        assertThat(manager.capture(project(), title())).isEqualTo(refusedCapture());
    }

    @Test
    void shouldCreateNothingWhenTheTemplateCannotBeRead() {
        when(templates.templateOf(project())).thenThrow(captureRefused());

        assertThat(manager.capture(project(), title())).isEqualTo(refusedCapture());
        verify(captures, never()).create(any(), any(), any());
    }
}
