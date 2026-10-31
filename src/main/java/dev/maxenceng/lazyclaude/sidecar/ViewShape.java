package dev.maxenceng.lazyclaude.sidecar;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** Which written record is a sealed type's variant and what its {@code kind} is, read by the writer and by the schema alike (ADR 0073). */
final class ViewShape {

    static final String KIND = "kind";
    private static final String VIEW = "View";

    private ViewShape() {}

    static boolean isVariantOfASealedType(Class<?> type) {
        return Stream.concat(Stream.of(type.getInterfaces()), Stream.ofNullable(type.getSuperclass())).anyMatch(Class::isSealed);
    }

    /** Whether a record is written as its one component: it has only that and is not a sealed type's variant. */
    static boolean isUnwrapped(Class<?> type) {
        return type.getRecordComponents().length == 1 && !isVariantOfASealedType(type);
    }

    /** Whether a type is written as a JSON object: a record of several components, a sealed type, a map, or a record that wraps one. */
    static boolean isWrittenAsObject(Class<?> type) {
        if (type.isRecord()) {
            return !isUnwrapped(type) || isWrittenAsObject(type.getRecordComponents()[0].getType());
        }
        return type.isSealed() || Map.class.isAssignableFrom(type);
    }

    /** The simple name without {@code View}, in lower camel case: {@code UnavailableBoardView} is {@code unavailableBoard}. */
    static String kindOf(Class<?> type) {
        String name = type.getSimpleName();
        String bare = name.endsWith(VIEW) && name.length() > VIEW.length() ? name.substring(0, name.length() - VIEW.length()) : name;
        return bare.substring(0, 1).toLowerCase(Locale.ROOT) + bare.substring(1);
    }
}
