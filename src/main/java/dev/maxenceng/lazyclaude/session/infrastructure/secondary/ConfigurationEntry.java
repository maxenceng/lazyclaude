package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import org.snakeyaml.engine.v2.nodes.Node;

/** One {@code key: value} of {@code config.yml}, with the line its key is on, counted from 1. */
record ConfigurationEntry(String key, int line, Node value) {}
