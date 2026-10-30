package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** A project's tickets, one group per status in lifecycle order, plus the unreadable tickets, and the project they were read from. */
public record Board(List<StatusGroup> groups, List<UnreadableTicket> unreadable, Project project) implements BoardReading {

    public Board {
        Assert.notNull("groups", groups);
        Assert.notNull("unreadable", unreadable);
        Assert.notNull("project", project);
    }

    public static BoardBuilder builder() {
        return new BoardBuilder();
    }

    public static final class BoardBuilder {
        private List<StatusGroup> groups;
        private List<UnreadableTicket> unreadable;
        private Project project;

        private BoardBuilder() {}

        public BoardBuilder groups(List<StatusGroup> groups) {
            this.groups = groups;
            return this;
        }

        public BoardBuilder unreadable(List<UnreadableTicket> unreadable) {
            this.unreadable = unreadable;
            return this;
        }

        public BoardBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public Board build() {
            return new Board(groups, unreadable, project);
        }
    }
}
