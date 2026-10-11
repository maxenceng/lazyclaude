package dev.maxenceng.lazyclaude.session.published;

import java.util.List;
import java.util.Optional;

/** A session's link to one ticket: a strong one its {@code /ticket} args named, or a weak one only its branch did. */
public sealed interface TicketLink permits StrongLink, WeakLink {

    LinkedTicketId ticket();

    /** The link among {@code links} to the ticket spelt {@code ticket}, if there is one. */
    static Optional<TicketLink> to(List<TicketLink> links, String ticket) {
        return links.stream().filter(link -> link.ticket().value().equals(ticket)).findFirst();
    }
}
