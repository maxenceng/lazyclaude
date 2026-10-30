# 67. A decision's blocked_by map wins over a map body that mentions it

Date: 2026-10-30

## Status

Accepted. Amends ADR 0064, which unions a decision's `blocked_by` maps with the maps whose body links it.

## Context

ADR 0064 takes a decision's map from both sources and refuses a decision that reaches two. Its consequences foresaw
the failure: lazyclaude's dry run refused LAZY-14, which is `blocked_by` LAZY-1 and linked from LAZY-1's body, because
LAZY-24's body mentions it in passing, "TamboUI's mouse reporting is off by default (LAZY-14)". The decision has one
map. The second one is a citation.

## Decision

- **`blocked_by` decides when it names a map; a map body decides only when `blocked_by` names none.** A decision
  `blocked_by` exactly one map is that map's sub-issue, whatever other map bodies mention it. It is refused when
  `blocked_by` names two or more maps, or when it names none and zero or several map bodies link it. Rejected: editing
  LAZY-24's line before migrating, which tidies a ticket by hand when ADR 0055 says the migration copies; counting only
  markdown links, which changes the rule next-suggestions was migrated under.

## Consequences

- `verify` derives the expected parent by the same rule, so a migration run before this change and verified after it
  would report a parent that differs. next-suggestions has no decision with a map in `blocked_by` (ADR 0064), so its
  migration is unaffected.
