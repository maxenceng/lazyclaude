package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.otherWarning;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.warning;
import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.twoWarnings;
import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.warnings;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.shared.domain.Warnings;

class WarningRepositoryTest {

    private final WarningRepository repository = new WarningRepository();
    private final List<Warnings> received = new ArrayList<>();

    @Test
    void shouldSayNothingToASubscriberWhenNothingWasRaised() {
        repository.subscribe(received::add);

        assertThat(received).as("no warning is not a warning to show").isEmpty();
    }

    @Test
    void shouldReplayAWarningRaisedBeforeTheSubscriber() {
        repository.raise(warning());

        repository.subscribe(received::add);

        assertThat(received).as("the first frame shows what start-up raised").containsExactly(warnings());
    }

    @Test
    void shouldReplayEveryWarningRaisedBeforeTheSubscriberInOneGo() {
        repository.raise(warning());
        repository.raise(otherWarning());

        repository.subscribe(received::add);

        assertThat(received).containsExactly(twoWarnings());
    }

    @Test
    void shouldHandASubscriberAWarningRaisedAfterwards() {
        repository.subscribe(received::add);

        repository.raise(warning());

        assertThat(received).containsExactly(warnings());
    }

    @Test
    void shouldHandASubscriberAllWarningsHeldEachTimeOneIsRaised() {
        repository.raise(warning());
        repository.subscribe(received::add);

        repository.raise(otherWarning());

        assertThat(received).containsExactly(warnings(), twoWarnings());
    }

    @Test
    void shouldKeepTheSameWarningRaisedTwice() {
        repository.raise(warning());
        repository.raise(warning());

        repository.subscribe(received::add);

        assertThat(received.getFirst().count()).as("each raise counts").isEqualTo(2);
    }

    @Test
    void shouldHandEverySubscriberTheWarnings() {
        List<Warnings> other = new ArrayList<>();
        repository.subscribe(received::add);
        repository.subscribe(other::add);

        repository.raise(warning());

        assertThat(received).containsExactly(warnings());
        assertThat(other).containsExactly(warnings());
    }

    @Test
    void shouldHandASubscriberTheWarningsLeftWhenOneIsWithdrawn() {
        repository.raise(warning());
        repository.raise(otherWarning());
        repository.subscribe(received::add);

        repository.withdraw(warning());

        assertThat(received).containsExactly(twoWarnings(), new Warnings(List.of(otherWarning())));
    }

    @Test
    void shouldTellAListenerWhenTheLastWarningIsWithdrawn() {
        repository.raise(warning());
        repository.subscribe(received::add);
        AtomicInteger told = new AtomicInteger();
        repository.onNoneHeld(told::incrementAndGet);

        repository.withdraw(warning());

        assertThat(told).hasValue(1);
        assertThat(received).as("no warning left is not a warning to show").containsExactly(warnings());
    }

    @Test
    void shouldSayNothingWhenAWarningThatIsNotHeldIsWithdrawn() {
        repository.raise(warning());
        repository.subscribe(received::add);
        AtomicInteger told = new AtomicInteger();
        repository.onNoneHeld(told::incrementAndGet);

        repository.withdraw(otherWarning());

        assertThat(received).containsExactly(warnings());
        assertThat(told).hasValue(0);
    }

    @Test
    void shouldTakeBackOnlyOneOfTwoEqualWarnings() {
        repository.raise(warning());
        repository.raise(warning());
        repository.subscribe(received::add);

        repository.withdraw(warning());

        assertThat(received.getLast().count()).isEqualTo(1);
    }

    @Test
    void shouldReplayNothingToASubscriberAfterTheLastWarningWasWithdrawn() {
        repository.raise(warning());
        repository.withdraw(warning());

        repository.subscribe(received::add);

        assertThat(received).isEmpty();
    }

    @Test
    void shouldHandASubscriberAWarningRaisedAfterTheLastWasWithdrawn() {
        repository.raise(warning());
        repository.withdraw(warning());
        repository.subscribe(received::add);

        repository.raise(otherWarning());

        assertThat(received).containsExactly(new Warnings(List.of(otherWarning())));
    }
}
