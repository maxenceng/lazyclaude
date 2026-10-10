# 20. JSON is parsed with Jackson 3, in secondary adapters only

Date: 2026-10-10

## Status

Accepted.

## Context

Sessions come from Claude Code's registry records and from `claude agents --json`, both JSON.
Transcripts and `gh --json` will be too. The backlog is YAML, read with SnakeYAML Engine (ADR
0012), which parses untyped maps by hand. Reading JSON the same way would repeat that mapping
code for every source, and a hand-written parser is no better for a format that is internal and
changes with Claude Code releases.

## Decision

- **Jackson 3** (`tools.jackson.core:jackson-databind`, package `tools.jackson.databind`) is the
  JSON library. Its annotations stay in `com.fasterxml.jackson.annotation`, as Jackson 3
  publishes them. The version is managed by `jackson-bom`.
- **It is used only in `infrastructure.secondary`.** An adapter reads the bytes into a private
  record that mirrors the JSON, then maps it into domain types. No domain, published or primary
  type carries a Jackson type or annotation.
- **`ArchitectureTest` enforces it**: nothing outside a secondary package may depend on
  `tools.jackson..` or `com.fasterxml.jackson..`. It was watched to fail on a Jackson mapper in
  a domain class before it was trusted.
- Unknown properties are ignored, which is Jackson 3's default: a field Claude Code adds is not a
  reason to stop reading its records.

## Consequences

- A second JSON source costs a record and a mapping, not a parser.
- A field Claude Code renames or drops reads as absent, and the domain refuses the session; the
  registry then counts as unusable (ADR 0021).
- Jackson 3 needs Java 17 or later and unchecked `JacksonException`s, which the adapters catch
  and turn into the context's own failures.
