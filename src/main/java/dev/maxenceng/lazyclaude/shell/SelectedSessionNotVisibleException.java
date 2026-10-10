package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The Sessions panel lost its selection: the selection is always on a row of the sessions it shows. */
final class SelectedSessionNotVisibleException extends IllegalStateException {

    SelectedSessionNotVisibleException(SessionId selected) {
        super("the selected session %s is not visible".formatted(selected.value()));
    }
}
