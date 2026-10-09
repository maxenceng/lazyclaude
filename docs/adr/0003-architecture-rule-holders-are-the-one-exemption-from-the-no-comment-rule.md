# 3. Architecture rule holders are the one exemption from the no-comment rule

Date: 2026-10-09

## Status

Accepted. Refines [2. Enforce architecture with executable rules](0002-enforce-architecture-with-executable-rules.md).

## Context

Nothing under `src/test/` carries a comment: a test is a named method, and the name has
already said what a comment would repeat — a comment beside it rots while the test stays green.

An ArchUnit `ArchRule` field is not a named method. It is a declarative constraint, and the
reason it exists has nowhere else in the file to live. Moving that reasoning out would put the
rules in one file and their justification in another.

## Decision

Nothing under `src/test/java` carries a comment, with one exemption: a file that holds
architecture rules, detected by `@AnalyzeClasses`. `scripts/check-test-comments.py` enforces
it in `make ci`, since comments do not survive into bytecode and ArchUnit cannot see them.

The check fails when *every* test is exempt: a rule that matched nothing has silently stopped
applying.

## Consequences

Reasoning that would have been a comment goes in the assertion's `.as(...)` description, where
a failure prints it, or in an ADR.
