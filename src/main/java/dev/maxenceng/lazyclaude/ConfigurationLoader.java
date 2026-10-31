package dev.maxenceng.lazyclaude;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import dev.maxenceng.lazyclaude.session.application.ConfigurationApplicationService;
import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeKeybindingsRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigurationRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.LaunchLogRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.NotifySendRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionRegistryRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TimerRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptCacheFile;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.WorktreeRepository;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;

/**
 * Reads what the user configured before anything else is built, then builds every context over it, whichever frontend is opened
 * (ADR 0077). The warnings it raises while reading are held for the frontend, which shows them once it is up.
 */
final class ConfigurationLoader {

    private static final String REGISTRY = ".claude/sessions";
    private static final String TRANSCRIPTS = ".claude/projects";
    private static final String XDG_CACHE_HOME = "XDG_CACHE_HOME";
    private static final String DEFAULT_CACHE_HOME = ".cache";
    private static final String XDG_STATE_HOME = "XDG_STATE_HOME";
    private static final String DEFAULT_STATE_HOME = ".local/state";
    private static final String CACHE_DIRECTORY = "lazyclaude";
    private static final String CACHE_FILE = "past-sessions.json";
    private static final List<String> CLAUDE = List.of("claude");
    private static final List<String> NOTIFY_SEND = List.of("notify-send");
    private static final List<String> GDBUS = List.of("gdbus");
    private static final Duration NOTIFY_PATIENCE = Duration.ofSeconds(5);
    private static final String REPAINT_THREAD = "session-repaint";

    private final Path home;
    private final Map<String, String> environment;
    private final WarningRepository warnings = new WarningRepository();

    ConfigurationLoader(Path home, Map<String, String> environment) {
        this.home = home;
        this.environment = environment;
    }

    /** What the user configured; a file that cannot be used is refused with a {@code DomainException} saying where and why. */
    ConfigurationView configuration() {
        return new ConfigurationQuery(new ConfigurationApplicationService(
                        new ConfigurationRepository(home, environment), new ClaudeKeybindingsRepository(home), warnings))
                .configuration();
    }

    /** Every context, wired over {@code configuration}. */
    Contexts contexts(ConfigurationView configuration) {
        return Contexts.of(warnings, sessionService(configuration.desktopNotifications()));
    }

    private SessionApplicationService sessionService(boolean desktopNotifications) {
        Path registry = home.resolve(REGISTRY);
        TranscriptRepository transcripts = new TranscriptRepository(
                home.resolve(TRANSCRIPTS),
                new TranscriptCacheFile(cacheHome(home, environment.get(XDG_CACHE_HOME))
                        .resolve(CACHE_DIRECTORY)
                        .resolve(CACHE_FILE)));
        return new SessionApplicationService(
                new SessionRegistryRepository(registry),
                transcripts,
                new LaunchLogRepository(stateHome(home, environment.get(XDG_STATE_HOME)).resolve(CACHE_DIRECTORY)),
                new SessionWatchRepository(registry),
                new TranscriptWatchRepository(home.resolve(TRANSCRIPTS), transcripts),
                warnings,
                new PtyHostRepository(CLAUDE, environment),
                new WorktreeRepository(),
                new NotifySendRepository(NOTIFY_SEND, GDBUS, NOTIFY_PATIENCE),
                desktopNotifications,
                new TimerRepository(Executors.newSingleThreadScheduledExecutor(task -> Thread.ofPlatform().name(REPAINT_THREAD).daemon().unstarted(task))),
                Clock.systemDefaultZone());
    }

    /** {@code $XDG_CACHE_HOME} when it is set to an absolute path, else {@code ~/.cache}, as the XDG base directory spec has it. */
    static Path cacheHome(Path home, String xdgCacheHome) {
        return Optional.ofNullable(xdgCacheHome)
                .filter(set -> !set.isBlank())
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElseGet(() -> home.resolve(DEFAULT_CACHE_HOME));
    }

    /** {@code $XDG_STATE_HOME} when it is set to an absolute path, else {@code ~/.local/state}, as the XDG base directory spec has it. */
    static Path stateHome(Path home, String xdgStateHome) {
        return Optional.ofNullable(xdgStateHome)
                .filter(set -> !set.isBlank())
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElseGet(() -> home.resolve(DEFAULT_STATE_HOME));
    }
}
