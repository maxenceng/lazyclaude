# 64. A decision belongs to the map whose body links it

Date: 2026-10-29

## Status

Accepted. Amends ADR 0055, which finds a decision's map through `blocked_by` alone.

## Context

ADR 0055 makes a decision a sub-issue of the map it reaches through `blocked_by`, and refuses a decision reaching two
maps or none. The backlogs as they are on `main` do not follow that: none of next-suggestions' 23 decisions, or
lazyclaude's 13, has its map in `blocked_by`. Some have an empty `blocked_by`, and the rest list only other decisions.
What each map does have is a body that links its decisions: IMPORT-1 names IMPORT-2 to 24, and LAZY-24 names all 13 LAZY
decisions.

## Decision

- **A decision's map is the map whose body links it, or the one it reaches through `blocked_by`.** A link to the
  decision's file, or its bare id, outside code spans counts, matched the same way the rewrite pass matches. The two
  sources are unioned. Exactly one map gives the sub-issue parent; none, or more than one, is still refused, and the
  refusal names the maps it found. Rejected: adding the map to the `blocked_by` of 36 decision files before migrating,
  which tidies the backlog by hand when ADR 0055 says the migration copies; a decision with no map edge going to the
  project's only map, which fails on lazyclaude's two maps.
- **Only a `decision` becomes a sub-issue.** A feature or chore linked from a map's body keeps its own place.

## Consequences

- `verify` derives the expected parent by the same rule from `main`.
- A map that mentions another map's decision in passing makes that decision refused. The dry run shows this before
  anything is written.
