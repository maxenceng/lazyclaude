package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AddRemoteControl;
import dev.maxenceng.lazyclaude.session.domain.ConfiguredRemoteControl;
import dev.maxenceng.lazyclaude.session.domain.ConflictingRemoteControl;
import dev.maxenceng.lazyclaude.session.domain.RemoteControlConfig;

/** What the popup over the main view shows when kitty refused remote control: the lines to add to {@code kitty.conf}, or the line that conflicts. */
public sealed interface RemoteControlConfigView permits AddRemoteControlView, ConflictingRemoteControlView {

    /** The popup for what {@code kitty.conf} needs; none when nothing in it is to be shown, as when kitty only needs a restart. */
    static Optional<RemoteControlConfigView> from(RemoteControlConfig config) {
        return switch (config) {
            case AddRemoteControl adding -> Optional.of(AddRemoteControlView.from(adding));
            case ConflictingRemoteControl conflicting -> Optional.of(ConflictingRemoteControlView.from(conflicting));
            case ConfiguredRemoteControl configured -> Optional.empty();
        };
    }
}
