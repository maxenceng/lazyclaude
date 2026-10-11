package dev.maxenceng.lazyclaude.session.published;

public final class TicketLinkFixture {

    private TicketLinkFixture() {}

    public static StrongLink strongLink(String ticket, String verb) {
        return StrongLink.builder()
                .ticket(new LinkedTicketId(ticket))
                .verb(new LinkVerb(verb))
                .build();
    }

    public static WeakLink weakLink(String ticket) {
        return new WeakLink(new LinkedTicketId(ticket));
    }
}
