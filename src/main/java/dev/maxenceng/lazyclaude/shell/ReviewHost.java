package dev.maxenceng.lazyclaude.shell;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What the review's keys ask of the shell they are pressed in: which panel has focus, stepping back out of the zoom or the review, a refusal for the key bar, what
 * follows an accepted submit, running on the render thread, and whether a review is still the one open.
 */
record ReviewHost(
        Supplier<Focus> focus,
        BooleanSupplier back,
        Consumer<String> refuse,
        Runnable submitted,
        Consumer<Runnable> onRenderThread,
        Predicate<ReviewPane> showing) {}
