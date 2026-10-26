package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

/** Says why a file operation failed in words a warning can carry; some exceptions' own message is only the path. */
final class FileFailure {

    private static final String ACCESS_DENIED = "access denied to %s";
    private static final String NO_SUCH_FILE = "no such file %s";
    private static final String NOT_UTF_8 = "not UTF-8 text";

    private FileFailure() {}

    static String causeOf(IOException failure) {
        return switch (failure) {
            case AccessDeniedException denied -> ACCESS_DENIED.formatted(denied.getFile());
            case NoSuchFileException missing -> NO_SUCH_FILE.formatted(missing.getFile());
            case CharacterCodingException _ -> NOT_UTF_8;
            default -> failure.getMessage();
        };
    }
}
