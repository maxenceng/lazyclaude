package dev.maxenceng.lazyclaude.session.published;

public final class TicketPluginFixture {

    private TicketPluginFixture() {}

    public static TicketPlugin ticketPlugin() {
        return new TicketPlugin("ddd-workflow");
    }

    public static TicketPlugin otherTicketPlugin() {
        return new TicketPlugin("my-tools");
    }
}
