package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.published.StatusVerbsFixture.inReviewDoneThenRespond;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConfiguredVerbsRepositoryTest {

    @Test
    void shouldGiveTheVerbsItWasHanded() {
        assertThat(new ConfiguredVerbsRepository(List.of(inReviewDoneThenRespond())).configured())
                .containsExactly(inReviewDoneThenRespond());
    }
}
