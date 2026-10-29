package dev.maxenceng.lazyclaude.shell;

/**
 * What takes every key and every mouse event while it is open, in the order it answers when more than one is: the keys, the mouse and
 * the render all read it, so a new popup is listed once (ADR 0051).
 */
enum Modal {
    QUIT_PROMPT,
    REMOVAL_PROMPT,
    SUBMIT_PROMPT,
    RESPOND_PROMPT,
    VERB_PICKER,
    COMMENT_BOX
}
