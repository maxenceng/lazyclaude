package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.tamboui.layout.Rect;

/** A drag a forwarded press started in a hosted session's pane: the session it belongs to, and the pane its moves and release are clamped to. */
record HostedDrag(SessionId session, Rect pane) {}
