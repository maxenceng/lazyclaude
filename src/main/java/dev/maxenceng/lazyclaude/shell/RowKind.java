package dev.maxenceng.lazyclaude.shell;

/** What a row of the Board, Sessions or Docs panel stands for. */
enum RowKind {
    GROUP,
    TICKET,
    UNREADABLE,
    UNAVAILABLE,
    FOLDER,
    DOCUMENT,
    SESSION,
    OLDER,
    HEADER,
    NO_SESSION,
    NEW_SESSION,
    STARTING
}
