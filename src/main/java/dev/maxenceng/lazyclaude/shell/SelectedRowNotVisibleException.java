package dev.maxenceng.lazyclaude.shell;

/** The Board panel lost its selection: only folding a header hides rows, and the selection is then on it. */
final class SelectedRowNotVisibleException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    SelectedRowNotVisibleException(RowKey selected) {
        super("the selected row %s is not visible".formatted(selected));
    }
}
