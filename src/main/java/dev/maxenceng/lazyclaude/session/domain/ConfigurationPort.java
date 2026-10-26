package dev.maxenceng.lazyclaude.session.domain;

/** Where lazyclaude's own configuration is read from. */
public interface ConfigurationPort {

    /**
     * The configuration, the defaults when there is no file.
     *
     * @throws ConfigurationRefusedException when the file is not valid, naming the file and the line; nothing is read of it
     */
    Configuration configuration();
}
