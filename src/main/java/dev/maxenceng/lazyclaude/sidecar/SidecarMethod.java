package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;

/** One protocol method: the params of a call in, its answer out. */
@FunctionalInterface
interface SidecarMethod {

    SidecarAnswer call(JsonNode params);
}
