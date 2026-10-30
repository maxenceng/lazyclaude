package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.blankTemplate;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.noTemplate;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateSections;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateWithFrontMatter;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateWithOnlyFrontMatter;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateWithUnclosedFrontMatter;
import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateWithoutFrontMatter;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;

class TicketTemplateTest {

    @Test
    void shouldTakeTheSectionsAfterTheFrontMatter() {
        assertThat(templateWithFrontMatter().sections()).isEqualTo(new Body(templateSections()));
    }

    @Test
    void shouldTakeAFileWithoutFrontMatterWhole() {
        assertThat(templateWithoutFrontMatter().sections()).isEqualTo(new Body(templateSections()));
    }

    @Test
    void shouldHaveNoSectionsWhenOnlyTheFrontMatterIsThere() {
        assertThat(templateWithOnlyFrontMatter().sections()).isEqualTo(new NoBody());
    }

    @Test
    void shouldHaveNoSectionsWhenTheFileIsBlank() {
        assertThat(blankTemplate().sections()).isEqualTo(new NoBody());
    }

    @Test
    void shouldHaveNoSectionsWhenThereIsNoTemplate() {
        assertThat(noTemplate().sections()).isEqualTo(new NoBody());
    }

    @Test
    void shouldNotTreatAnUnclosedFrontMatterAsOne() {
        assertThat(templateWithUnclosedFrontMatter().sections())
                .isEqualTo(new Body("---\nname: Ticket\n## Notes"));
    }
}
