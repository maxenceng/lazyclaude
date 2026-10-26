package dev.maxenceng.lazyclaude.session.published;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * The chord of each of the seven reserved actions, an action left out keeping its default. A chord is taken before {@code claude} sees it inside a hosted session, so
 * two actions cannot share one: the second could never be pressed.
 */
public record ReservedChords(Map<ReservedAction, Chord> bound) {

    public ReservedChords {
        Assert.notNull("bound chords", bound);
        Map<ReservedAction, Chord> kept = merged(defaultChords(), bound);
        refuseDuplicates(kept);
        bound = kept;
    }

    public static ReservedChords defaults() {
        return new ReservedChords(Map.of());
    }

    /** These chords with {@code changes} in place of the ones they name, every other action keeping its own. */
    public ReservedChords rebinding(Map<ReservedAction, Chord> changes) {
        return new ReservedChords(merged(bound, changes));
    }

    public Chord of(ReservedAction action) {
        return bound.get(action);
    }

    /** The action {@code pressed} is the chord of, none when it is free to reach {@code claude}. */
    public Optional<ReservedAction> actionOf(TerminalKey pressed) {
        return Arrays.stream(ReservedAction.values())
                .filter(action -> of(action).isPressedBy(pressed))
                .findFirst();
    }

    /** The actions whose chord is among {@code others}, in the order of {@link ReservedAction}. */
    public List<ReservedAction> clashingWith(List<Chord> others) {
        return Arrays.stream(ReservedAction.values())
                .filter(action -> others.contains(of(action)))
                .toList();
    }

    private static Map<ReservedAction, Chord> defaultChords() {
        return Arrays.stream(ReservedAction.values())
                .collect(Collectors.toUnmodifiableMap(Function.identity(), ReservedAction::defaultChord));
    }

    private static Map<ReservedAction, Chord> merged(Map<ReservedAction, Chord> base, Map<ReservedAction, Chord> changes) {
        return Stream.concat(base.entrySet().stream(), changes.entrySet().stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, later) -> later),
                        Collections::unmodifiableMap));
    }

    private static void refuseDuplicates(Map<ReservedAction, Chord> chords) {
        Arrays.stream(ReservedAction.values())
                .flatMap(first -> Arrays.stream(ReservedAction.values())
                        .filter(second -> first.ordinal() < second.ordinal())
                        .filter(second -> chords.get(first).equals(chords.get(second)))
                        .map(second -> new DuplicateChordException(chords.get(first), first, second)))
                .findFirst()
                .ifPresent(duplicate -> {
                    throw duplicate;
                });
    }
}
