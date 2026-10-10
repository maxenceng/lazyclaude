package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Finds the project a directory belongs to. */
public interface ProjectLocator {

    /** @throws ProjectNotFoundException with the reason there is none, written for the person who started lazyclaude */
    Project locate(Path from);
}
