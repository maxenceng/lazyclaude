package dev.maxenceng.lazyclaude.shared.domain;


/** Finds the project a working directory belongs to. */
public interface ProjectLocatorPort {

    /**
     * @throws ProjectNotFoundException with the reason there is none
     * @throws ProjectUnavailableException when the lookup itself failed, so the answer may differ next time
     */
    Project locate(WorkingDirectory from);
}
