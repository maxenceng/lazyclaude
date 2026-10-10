package dev.maxenceng.lazyclaude.shared.domain;


/** Finds the project a working directory belongs to. */
public interface ProjectLocatorPort {

    /** @throws ProjectNotFoundException with the reason there is none */
    Project locate(WorkingDirectory from);
}
