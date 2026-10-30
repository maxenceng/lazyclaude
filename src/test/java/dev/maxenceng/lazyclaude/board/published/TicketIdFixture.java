package dev.maxenceng.lazyclaude.board.published;

public final class TicketIdFixture {

    private TicketIdFixture() {}

    public static TicketId ticketId() {
        return new TicketId("1");
    }

    public static TicketId secondTicketId() {
        return new TicketId("2");
    }

    public static TicketId tenthTicketId() {
        return new TicketId("10");
    }

    public static TicketId ticketIdNumbered(int number) {
        return new TicketId(String.valueOf(number));
    }
}
