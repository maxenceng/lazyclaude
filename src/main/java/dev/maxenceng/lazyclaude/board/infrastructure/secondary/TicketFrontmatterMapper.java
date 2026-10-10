package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.Map;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

/**
 * Reads a ticket's YAML frontmatter into a {@link TicketFrontmatter}. SnakeYAML Engine answers
 * untyped maps; this is the one place that reads one.
 */
final class TicketFrontmatterMapper {

    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String CONTEXT = "context";
    private static final String TYPE = "type";

    private final Load yaml = new Load(LoadSettings.builder().build());

    /** @throws YamlEngineException when the frontmatter is not valid YAML */
    TicketFrontmatter read(String frontmatter) {
        Map<?, ?> fields = yaml.loadFromString(frontmatter) instanceof Map<?, ?> loaded ? loaded : Map.of();
        return new TicketFrontmatter(
                text(fields.get(ID)), text(fields.get(STATUS)), text(fields.get(CONTEXT)), text(fields.get(TYPE)));
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
