package dev.maxenceng.lazyclaude.board.domain;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket template as the project wrote it, front matter and all. */
public record TemplateFile(String text) implements TicketTemplate {

    private static final Pattern FRONT_MATTER = Pattern.compile("\\A---[ \\t]*\\R.*?\\R---[ \\t]*(?:\\R|\\z)", Pattern.DOTALL);

    public TemplateFile {
        Assert.notNull("template text", text);
    }

    @Override
    public TicketBody sections() {
        String sections = FRONT_MATTER.matcher(text).replaceFirst("").strip();
        return sections.isEmpty() ? new NoBody() : new Body(sections);
    }
}
