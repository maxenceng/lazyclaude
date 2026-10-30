package dev.maxenceng.lazyclaude.board.domain;

public final class TicketTemplateFixture {

    private TicketTemplateFixture() {}

    public static TicketTemplate templateWithFrontMatter() {
        return new TemplateFile("---\nname: Ticket\nabout: A unit of work\n---\n\n## Acceptance criteria\n\n_Not analysed yet._\n\n## Notes\n");
    }

    public static String templateSections() {
        return "## Acceptance criteria\n\n_Not analysed yet._\n\n## Notes";
    }

    public static TicketTemplate templateWithoutFrontMatter() {
        return new TemplateFile("## Acceptance criteria\n\n_Not analysed yet._\n\n## Notes\n");
    }

    public static TicketTemplate templateWithOnlyFrontMatter() {
        return new TemplateFile("---\nname: Ticket\n---\n");
    }

    public static TicketTemplate blankTemplate() {
        return new TemplateFile("  \n");
    }

    public static TicketTemplate templateWithUnclosedFrontMatter() {
        return new TemplateFile("---\nname: Ticket\n## Notes\n");
    }

    public static TicketTemplate noTemplate() {
        return new NoTemplate();
    }
}
