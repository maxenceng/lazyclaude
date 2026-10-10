---
id: LAZY-22
status: draft
context: cross-context
type: feature
created: 2026-10-11
---

# A warning raised after start-up shows on the screen instead of printing over the TUI

## Acceptance criteria

_Not analysed yet — `/ticket refine LAZY-22`._

## Model decision

_Not analysed yet — `/ticket refine LAZY-22`._

## Design direction

_Not analysed yet — `/ticket refine LAZY-22`._

## Glossary impact

_Not analysed yet._

## Notes

Raised in the review of [SESSION-1](SESSION-1-live-sessions.md). `LiveSessionRepository`'s fallback warning and `DocsWatchRepository`'s watch warning both print to stderr. One that first fires after start-up lands on top of the drawn screen. The fix is suggested to sit in the shell, as a status-line message both warnings go through.
