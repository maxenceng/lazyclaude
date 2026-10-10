package dev.maxenceng.lazyclaude.board.domain;

import java.util.Objects;

/** One backlog ticket as the board shows it. */
public record Ticket(TicketId id, String title, Status status, String context, String type) {

    public Ticket {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        title = title == null || title.isBlank() ? id.value() : title.strip();
        context = context == null ? "" : context;
        type = type == null ? "" : type;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private TicketId id;
        private String title;
        private Status status;
        private String context;
        private String type;

        private Builder() {}

        public Builder id(TicketId id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Ticket build() {
            return new Ticket(id, title, status, context, type);
        }
    }
}
