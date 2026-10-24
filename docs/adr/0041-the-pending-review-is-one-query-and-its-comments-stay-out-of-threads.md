# 41. The pending review is one query, and its comments stay out of threads

Date: 2026-10-24

## Status

Accepted. Builds on ADR 0009.

## Context

A write to the pending review needs GitHub's id of the pull request (to start the review), of the review (to add to it), and, to
warn of a moved head, the commit the review was written on beside the head now. GitHub also lists a pending comment as a comment of
a review thread, to its author, with the state `PENDING`; read as a thread it would show twice, as a `◆` thread and a `✎` comment,
and count toward `◆n`. A pending comment has no side of its own: the side belongs to the thread it sits in.

## Decision

- **One GraphQL query reads the pull request's `id` and `headRefOid`, `reviews(states: PENDING, first: 1)` with its id, body and
  `commit.oid`, and the review threads with their comments' ids, states and bodies.** A pending comment is a comment with the state
  `PENDING`: the first of its thread starts a thread on the diff, any later one is a reply to it. Where a thread has no line, as
  after the head moved, the line it was written on stands.
- **A write is answered with the pending review read again**, so that the screen shows what GitHub holds and a retry after a lost
  answer finds what was already written.
- **The thread read leaves out pending comments, and threads left with none.** Comments is the one place a pending comment is
  listed, and Files counts them as `✎n`, apart from `◆n`.

## Consequences

- Each save is a read and a write, and a read again; the first save of a review is one call more.
- A pending comment on a whole file, written on github.com, has no line and is left out until one is seen.
- Only a thread's first 50 comments are read, as for threads; a pending reply after the fiftieth is not seen.
