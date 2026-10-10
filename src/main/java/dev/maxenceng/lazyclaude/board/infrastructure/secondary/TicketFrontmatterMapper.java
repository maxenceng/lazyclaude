package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;

import dev.maxenceng.lazyclaude.board.domain.Frontmatter;
import dev.maxenceng.lazyclaude.board.domain.FrontmatterEntry;
import dev.maxenceng.lazyclaude.board.published.FrontmatterKey;
import dev.maxenceng.lazyclaude.board.published.FrontmatterValue;

/**
 * Reads a ticket's YAML frontmatter into a {@link TicketFrontmatter}. SnakeYAML Engine answers
 * its nodes untyped; this is the one place that reads them. It reads the nodes rather than the
 * resolved values, so a scalar keeps the text the file wrote. It still loads the values first,
 * because only loading refuses what the board refused before it read nodes: a key written twice.
 */
final class TicketFrontmatterMapper {

    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String CONTEXT = "context";
    private static final String TYPE = "type";
    private static final Set<String> MODELLED = Set.of(ID, STATUS, CONTEXT, TYPE);
    private static final String LIST_OPEN = "[";
    private static final String LIST_CLOSE = "]";
    private static final String MAP_OPEN = "{";
    private static final String MAP_CLOSE = "}";
    private static final String PAIR_SEPARATOR = ": ";
    private static final String ITEM_SEPARATOR = ", ";
    private static final String LINE_JOIN = " ";
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    private final Load values = new Load(LoadSettings.builder().build());
    private final Compose nodes = new Compose(LoadSettings.builder().build());

    /** @throws YamlEngineException when the frontmatter is not valid YAML */
    TicketFrontmatter read(String frontmatter) {
        values.loadFromString(frontmatter);
        List<NodeTuple> tuples = nodes.composeString(frontmatter)
                .filter(MappingNode.class::isInstance)
                .map(MappingNode.class::cast)
                .map(MappingNode::getValue)
                .orElse(List.of());
        return new TicketFrontmatter(
                field(tuples, ID), field(tuples, STATUS), field(tuples, CONTEXT), field(tuples, TYPE), otherKeys(tuples));
    }

    private static Frontmatter otherKeys(List<NodeTuple> tuples) {
        List<FrontmatterEntry> entries = new ArrayList<>();
        for (NodeTuple tuple : tuples) {
            String key = display(tuple.getKeyNode());
            if (!key.isBlank() && !MODELLED.contains(key)) {
                entries.add(FrontmatterEntry.builder()
                        .key(new FrontmatterKey(key))
                        .value(new FrontmatterValue(display(tuple.getValueNode())))
                        .build());
            }
        }
        return new Frontmatter(entries);
    }

    private static String field(List<NodeTuple> tuples, String key) {
        return tuples.stream()
                .filter(tuple -> display(tuple.getKeyNode()).equals(key))
                .map(NodeTuple::getValueNode)
                .findFirst()
                .flatMap(TicketFrontmatterMapper::present)
                .map(TicketFrontmatterMapper::display)
                .orElse(null);
    }

    private static Optional<Node> present(Node value) {
        boolean isNull = value instanceof ScalarNode && value.getTag().equals(Tag.NULL);
        return isNull ? Optional.empty() : Optional.of(value);
    }

    private static String display(Node node) {
        return display(node, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /** @throws YamlEngineException when an alias makes a value contain itself, which has no text */
    private static String display(Node node, Set<Node> enclosing) {
        if (!enclosing.add(node)) {
            throw new YamlEngineException("a value contains itself through an alias");
        }
        String text = switch (node) {
            case ScalarNode scalar -> oneRow(scalar.getValue());
            case SequenceNode sequence ->
                sequence.getValue().stream()
                        .map(item -> display(item, enclosing))
                        .collect(Collectors.joining(ITEM_SEPARATOR, LIST_OPEN, LIST_CLOSE));
            case MappingNode mapping ->
                mapping.getValue().stream()
                        .map(tuple -> display(tuple.getKeyNode(), enclosing)
                                + PAIR_SEPARATOR
                                + display(tuple.getValueNode(), enclosing))
                        .collect(Collectors.joining(ITEM_SEPARATOR, MAP_OPEN, MAP_CLOSE));
            default -> node.toString();
        };
        enclosing.remove(node);
        return text;
    }

    /** A value written over several lines shows on one row: its lines, stripped, joined by a space. */
    private static String oneRow(String text) {
        if (!LINE_BREAK.matcher(text).find()) {
            return text;
        }
        return text.lines().map(String::strip).filter(line -> !line.isEmpty()).collect(Collectors.joining(LINE_JOIN));
    }
}
