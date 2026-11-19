package dev.maxenceng.lazyclaude.session.domain;

public final class TicketDescriptionFixture {

    private TicketDescriptionFixture() {}

    public static TicketDescription ticketDescription() {
        return new TicketDescription("add a \"dark mode\" toggle");
    }
}
