package dev.maxenceng.lazyclaude.sidecar;

import java.util.Locale;
import java.util.stream.Stream;

/** How a View is told apart on the wire, read by the writer and by the schema alike so the two cannot drift (ADR 0050). */
final class ViewShape {

    static final String KIND = "kind";
    private static final String VIEW = "View";

    private ViewShape() {}

    static boolean isVariantOfASealedType(Class<?> type) {
        return Stream.concat(Stream.of(type.getInterfaces()), Stream.ofNullable(type.getSuperclass())).anyMatch(Class::isSealed);
    }

    /** The simple name without {@code View}, in lower camel case: {@code UnavailableBoardView} is {@code unavailableBoard}. */
    static String kindOf(Class<?> type) {
        String name = type.getSimpleName();
        String bare = name.endsWith(VIEW) && name.length() > VIEW.length() ? name.substring(0, name.length() - VIEW.length()) : name;
        return bare.substring(0, 1).toLowerCase(Locale.ROOT) + bare.substring(1);
    }
}
