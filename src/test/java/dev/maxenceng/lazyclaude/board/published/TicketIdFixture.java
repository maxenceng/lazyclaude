package dev.maxenceng.lazyclaude.board.published;

public final class TicketIdFixture {

    private TicketIdFixture() {}

    public static TicketId ticketId() {
        return new TicketId("PROJ-1");
    }

    public static TicketId secondTicketId() {
        return new TicketId("PROJ-2");
    }

    public static TicketId tenthTicketId() {
        return new TicketId("PROJ-10");
    }
}
