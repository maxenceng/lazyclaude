package dev.maxenceng.lazyclaude.board.published;

public final class TicketBranchFixture {

    private TicketBranchFixture() {}

    public static TicketBranch ticketBranch() {
        return new TicketBranch("PROJ-1");
    }

    public static TicketBranch secondTicketBranch() {
        return new TicketBranch("PROJ-2");
    }

    public static TicketBranch slugTicketBranch() {
        return new TicketBranch("PROJ-1-first-slice");
    }

    public static TicketBranch dottedTicketBranch() {
        return new TicketBranch("PROJ-1-v1.2-notes");
    }
}
