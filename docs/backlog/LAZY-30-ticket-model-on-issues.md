---
id: LAZY-30
status: todo
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-29]
---

# How does a ticket map onto a GitHub issue?

## Question

How does a ticket map onto a GitHub issue?

- the id: the issue number, a `CONTEXT-n` kept in the title, or both — and the ticket branch's name, which today is the filename;
- status (`draft` … `done`): open/closed plus a label, or a Projects field; what replaces reading status from the ticket branch's copy (ADRs 0028, BOARD-3's rule), now that status no longer rides along in the branch;
- context and type, the body's sections (Model decision, Design direction, acceptance criteria), `map`/`decision` and `blocked_by`;
- what has no natural home on an issue, and where it goes.

## Kind

grilling

## Resolution

_Not resolved yet._
