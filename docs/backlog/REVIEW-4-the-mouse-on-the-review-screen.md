---
id: REVIEW-4
status: in-review
context: review
type: feature
tracker_id: 102
created: 2026-10-27
---

# The mouse selects files, comments and diff lines on the review screen, and drags a range

## Acceptance criteria

- [ ] A click on a row of Files or Comments focuses that panel and selects the row; a second click goes to its line,
      or opens the pending comment, as `enter` does.
- [ ] A click on a diff line focuses the diff and moves the line cursor there; a second click edits that line's
      pending comment, as `enter` does.
- [ ] A drag across diff lines selects the range from the press's line to the release's, as `v` does, clamped to the
      diff when the release lands outside it; `c` then comments on that range.
- [ ] The wheel over Files or Comments moves the selection a row a notch, over the diff it scrolls three lines; either
      focuses the pane under the pointer.
- [ ] A click in the comment box or on the Review panel focuses nothing new and submits nothing; `S` stays the only
      submit.
- [ ] Every key does what it did before.

## Model decision

The shell's review screen, on BOARD-7's hit map and second click (ADR 0051). Nothing in `review`'s domain changes: the
range a drag selects is the one `v` already makes.

## Design direction

No layout change; the selected range shows as `v`'s does.

## Glossary impact

none

## Notes

Needs BOARD-7's hit map and global handler first.
