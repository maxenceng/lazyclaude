package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;

/** One protocol method: the params of a call in, its result out. */
@FunctionalInterface
interface SidecarMethod {

    JsonNode call(JsonNode params);
}
