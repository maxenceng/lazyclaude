package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.domain.TicketCaptureFixture.captureRefused;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.noTemplate;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.blankTitlePayload;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.capturePayload;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.capturedTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.refusedTicketCaptureView;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.application.TicketCaptureApplicationService;
import dev.maxenceng.lazyclaude.board.domain.TicketCapturePort;
import dev.maxenceng.lazyclaude.board.domain.TicketTemplatePort;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.error.domain.AssertionException;

@ExtendWith(MockitoExtension.class)
class TicketCaptureCommandTest {

    @Mock
    private TicketCapturePort captures;

    @Mock
    private TicketTemplatePort templates;

    private TicketCaptureCommand command;

    @BeforeEach
    void createCommand() {
        command = new TicketCaptureCommand(new TicketCaptureApplicationService(captures, templates));
    }

    @Test
    void shouldAnswerTheNumberGitHubGaveTheTicket() {
        when(templates.templateOf(project())).thenReturn(noTemplate());
        when(captures.create(any(), any(), any())).thenReturn(ticketId());

        assertThat(command.capture(capturePayload())).isEqualTo(capturedTicketView());
    }

    @Test
    void shouldAnswerWhyTheTicketWasRefused() {
        when(templates.templateOf(project())).thenReturn(noTemplate());
        when(captures.create(any(), any(), any())).thenThrow(captureRefused());

        assertThat(command.capture(capturePayload())).isEqualTo(refusedTicketCaptureView());
    }

    @Test
    void shouldRefuseABlankTitleWithoutAskingGitHub() {
        assertThat(command.capture(blankTitlePayload()))
                .isInstanceOfSatisfying(RefusedTicketCaptureView.class, refused ->
                        assertThat(refused.reason().value()).as("the reason names the field").contains("title"));
        verifyNoInteractions(captures, templates);
    }

    @Test
    void shouldNotAnswerAnInvalidValueMetAfterGitHubWasAskedAsARefusal() {
        when(templates.templateOf(project())).thenReturn(noTemplate());
        when(captures.create(any(), any(), any())).thenAnswer(_ -> new TicketId("0"));

        assertThatThrownBy(() -> command.capture(capturePayload()))
                .as("a refusal invites enter again, which would create the ticket twice")
                .isInstanceOf(AssertionException.class);
    }
}
