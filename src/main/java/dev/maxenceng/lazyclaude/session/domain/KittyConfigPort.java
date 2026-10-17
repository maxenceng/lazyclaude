package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

/** Where kitty's configuration is found, read and added to. */
public interface KittyConfigPort {

    /** The {@code kitty.conf} kitty reads: {@code $KITTY_CONFIG_DIRECTORY/kitty.conf}, else {@code ~/.config/kitty/kitty.conf}. */
    KittyConfigFile locate();

    /**
     * The settings of {@code keys} that {@code file} makes, and every file it {@code include}s or {@code globinclude}s makes, in the
     * order kitty applies them. A file that does not exist makes none.
     *
     * @throws KittyConfigUnreadableException when a file that exists cannot be read, with the reason
     */
    KittySettings settings(KittyConfigFile file, List<ConfigKey> keys);

    /**
     * Appends the text of {@code adding} to its file, creating the file when it is missing.
     *
     * @throws KittyConfigUnwritableException when the file cannot be written, with the reason
     */
    void append(AddRemoteControl adding);
}
