package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;

public record Node(List<Node> children) {}
