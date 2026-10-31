package dev.maxenceng.lazyclaude.board.domain;

/** What asking for a working directory's board gives: the project's board reading, or why the directory belongs to no project. */
public sealed interface BoardLookup permits BoardReading, NoProject {}
