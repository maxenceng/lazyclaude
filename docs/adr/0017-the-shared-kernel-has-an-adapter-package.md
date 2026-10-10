# 17. The shared kernel has an adapter package, `shared.infrastructure.secondary`

Date: 2026-10-10

## Status

Accepted. Extends ADR 0010.

## Context

`Utf8Text` decodes a file's bytes as strict UTF-8 without a byte order mark. `board` and `docs`
both read files that way, so it went into the shared kernel. Only their secondary adapters use
it, and ADR 0010 says the kernel's `shared.domain` holds types a context's *domain* needs. Two
copies of the same ten lines would drift, and a domain-free helper in `shared.domain` makes that
rule untrue.

## Decision

- **Helpers that more than one context's secondary adapters share live in
  `shared.infrastructure.secondary`.** `Utf8Text` is the first.
- **The existing rules already fence it in.** A domain may depend only on `java..`, domains and
  `published`, and the application layer may not reach `..infrastructure..`, so only adapters can
  use it.
- `shared.domain` stays as ADR 0010 has it: types a domain needs, nothing outside `java..`.

## Consequences

- An adapter helper that two contexts repeat moves here rather than being copied.
- It is still the kernel: every addition couples every context that uses it. A helper with one
  user stays in that context's adapter.
