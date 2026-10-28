package dev.maxenceng.lazyclaude.session.published;

public final class TicketLinkFixture {

    private TicketLinkFixture() {}

    public static TicketLink ticketLink(String ticket, String verb) {
        return TicketLink.builder()
                .ticket(new LinkedTicketId(ticket))
                .verb(new LinkVerb(verb))
                .build();
    }
}
