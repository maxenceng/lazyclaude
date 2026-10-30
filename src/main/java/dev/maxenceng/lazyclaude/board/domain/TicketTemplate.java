package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketBody;

/** The project's template for a new ticket: the file under {@code .github/ISSUE_TEMPLATE}, or nothing when the project has none. */
public sealed interface TicketTemplate permits TemplateFile, NoTemplate {

    /** What a new ticket starts with: the template's sections, which is everything after its front matter. */
    TicketBody sections();
}
