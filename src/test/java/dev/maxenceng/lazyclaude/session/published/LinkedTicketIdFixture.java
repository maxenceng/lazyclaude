package dev.maxenceng.lazyclaude.session.published;

import java.util.Arrays;
import java.util.List;

public final class LinkedTicketIdFixture {

    private LinkedTicketIdFixture() {}

    public static LinkedTicketId linkedTicketId() {
        return new LinkedTicketId("PROJ-1");
    }

    public static LinkedTicketId secondLinkedTicketId() {
        return new LinkedTicketId("PROJ-2");
    }

    public static LinkedTicketId linkedTicketId(String value) {
        return new LinkedTicketId(value);
    }

    public static List<LinkedTicketId> linkedTicketIds(String... values) {
        return Arrays.stream(values).map(LinkedTicketId::new).toList();
    }
}
