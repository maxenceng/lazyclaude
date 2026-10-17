package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Adds the lines that turn remote control on to {@code kitty.conf}, as the user answered yes to the offer. The file is read
 * again first, so a key set another way since the offer is a conflict and nothing is edited. Kitty cannot reload either
 * option, so every outcome says it needs a restart, or why nothing changed.
 */
public final class KittyConfigManager {

    private static final String UPDATED = "kitty.conf updated; restart kitty to turn remote control on.";
    private static final String NEEDS_RESTART = "kitty.conf already allows remote control, so kitty needs a restart.";
    private static final String CONFLICT = "kitty.conf not changed: line %d of %s sets %s to something else.";
    private static final String CANNOT_UPDATE = "cannot update kitty.conf: %s";

    private final KittyConfigPort config;
    private final WarningPort warnings;

    public KittyConfigManager(KittyConfigPort config, WarningPort warnings) {
        this.config = config;
        this.warnings = warnings;
    }

    /** Appends what is still missing and says kitty needs a restart; a conflict or a failure is a warning and edits nothing. */
    public void add() {
        try {
            KittyConfigFile file = config.locate();
            switch (config.settings(file, KittySettings.remoteControlKeys()).remoteControl(file)) {
                case AddRemoteControl adding -> {
                    config.append(adding.file(), adding.lines());
                    warnings.raise(new Warning(UPDATED));
                }
                case ConflictingRemoteControl conflicting -> warnings.raise(new Warning(CONFLICT.formatted(
                        conflicting.found().place().line().value(),
                        conflicting.found().place().file().path(),
                        conflicting.found().line().key().value())));
                case ConfiguredRemoteControl configured -> warnings.raise(new Warning(NEEDS_RESTART));
            }
        } catch (KittyConfigUnreadableException unreadable) {
            cannotUpdate(unreadable.getMessage());
        } catch (KittyConfigUnwritableException unwritable) {
            cannotUpdate(unwritable.getMessage());
        }
    }

    private void cannotUpdate(String reason) {
        warnings.raise(new Warning(CANNOT_UPDATE.formatted(reason)));
    }
}
