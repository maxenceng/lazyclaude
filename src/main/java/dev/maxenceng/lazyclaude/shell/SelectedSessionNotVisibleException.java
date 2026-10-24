package dev.maxenceng.lazyclaude.shell;

/** The Sessions panel lost its selection: the selection is always on a row of the sessions it shows. */
final class SelectedSessionNotVisibleException extends IllegalStateException {

    SelectedSessionNotVisibleException(RowKey selected) {
        super("the selected session row %s is not visible".formatted(selected));
    }
}
