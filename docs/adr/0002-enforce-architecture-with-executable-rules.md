# 2. Enforce architecture with executable rules, not prose

Date: 2026-10-09

## Status

Accepted

## Context

A rule described in an instructions file is re-read on every request and enforced on none.
Nothing fails when it is broken, so it erodes one reasonable-looking exception at a time.

## Decision

Express architecture rules as tests wherever they can be. `ArchitectureTest` (ArchUnit) holds
them: inward dependencies, a plain-Java domain, the terminal confined to the shell and primary
adapters, side effects (processes, files, network) confined to secondary adapters, contexts
reaching each other only through `published`, and nothing depending on the shell.

There is no Spring here, so there is no Spring Modulith: context boundaries are an ArchUnit
condition over package names instead. A context is a direct subpackage of
`dev.maxenceng.lazyclaude` other than `shell` and `shared`.

Prose is kept for what tests cannot express — which context a concept belongs to, the
ubiquitous language — and lives in `docs/`, not in always-resident context.

## Consequences

Violations fail the build instead of surviving review. Each rule allows an empty match, since
the project starts with no context; every rule was watched to fail against a planted violation
before it was kept.

A rule that needs to change is changed in a commit, with an ADR.
