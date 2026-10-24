# 40. The review screen's left column holds a third panel, Review

Date: 2026-10-24

## Status

Accepted. Amends ADR 0039, which gives the left column of a review Files over Comments and nothing else.

## Context

Writing a review (ADR 0009) adds three things the screen must always show: the summary, how many comments are pending, and
whether the pull request's head moved since the pending review was written. They belong to no file and to no thread, so
neither Files nor Comments is the place for them.

## Decision

- **While a review is open the left column holds Files, Comments and Review, top to bottom.** Review has a fixed height of its
  own; Files and Comments share the rest. It is never focused: nothing in it is selected or acted on.
- **Review says**: the summary's first line, `n pending`, and, when the head moved, `written on <sha>,` and `head is now <sha>`
  in the warning colour. It says `no pending review`, `loading…` or `pending unknown (<reason>)` dim when there is nothing else.
- `alt+j` and `alt+k` still run between Files and Comments and no further.

## Consequences

- Files and Comments are shorter by the height of Review, on every terminal.
- A narrow terminal clips Review's rows, as it clips any panel's.
