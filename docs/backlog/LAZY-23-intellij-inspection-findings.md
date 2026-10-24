---
id: LAZY-23
status: draft
context: unassigned
type: chore
created: 2026-10-24
---

# Clear what IntelliJ's code inspection reports, and decide which of it the build should enforce

## Acceptance criteria

_not analysed yet_

## Model decision

_not analysed yet_

## Design direction

_not analysed yet_

## Glossary impact

_not analysed yet_

## Notes

Raised while closing [REVIEW-3](REVIEW-3-writing-a-review.md): "IntelliJ code inspection shows a lot of issues." No findings
were captured. The headless run (`idea inspect`) refuses to start while the IDE is open, so refine either exports the results
from the IDE (*Code → Inspect Code*, then export) or runs it with the IDE closed. Spans every context; refine settles the prefix.
