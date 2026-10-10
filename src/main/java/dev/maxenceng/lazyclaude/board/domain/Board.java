package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A project's tickets, one group per status in lifecycle order, plus the backlog files that could not be read. */
public record Board(List<StatusGroup> groups, List<UnreadableTicket> unreadable) {

    public Board {
        Assert.notNull("groups", groups);
        Assert.notNull("unreadable", unreadable);
    }

    public static BoardBuilder builder() {
        return new BoardBuilder();
    }

    public static final class BoardBuilder {
        private List<StatusGroup> groups;
        private List<UnreadableTicket> unreadable;

        private BoardBuilder() {}

        public BoardBuilder groups(List<StatusGroup> groups) {
            this.groups = groups;
            return this;
        }

        public BoardBuilder unreadable(List<UnreadableTicket> unreadable) {
            this.unreadable = unreadable;
            return this;
        }

        public Board build() {
            return new Board(groups, unreadable);
        }
    }
}
