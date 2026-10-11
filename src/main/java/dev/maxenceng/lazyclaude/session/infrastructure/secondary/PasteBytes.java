package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;

import dev.maxenceng.lazyclaude.session.published.PastedText;

/** A paste as the bytes a terminal sends: bracketed when the child turned bracketed paste on, so its newlines are text and do not submit the prompt. */
final class PasteBytes {

    private static final String START = "\u001b[200~";
    private static final String END = "\u001b[201~";
    private static final String CARRIAGE_RETURN = "\r";

    private PasteBytes() {}

    static byte[] of(PastedText paste, boolean bracketed) {
        String text = bracketed ? START + paste.value().replace(END, "") + END : typed(paste.value());
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String typed(String text) {
        return text.replace("\r\n", CARRIAGE_RETURN).replace("\n", CARRIAGE_RETURN);
    }
}
