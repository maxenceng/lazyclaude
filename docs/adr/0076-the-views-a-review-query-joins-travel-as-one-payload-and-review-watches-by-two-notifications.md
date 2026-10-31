# 76. The views a review query joins travel as one payload, and review watches by two notifications

Date: 2026-10-31

## Status

Accepted. Builds on ADR 0050, ADR 0073 and ADR 0074. Amends ADR 0074: a Payload may hold a sealed type. Amends ADR 0050: a
watch may push more than one `<context>/changed`-style notification.

## Context

`docs` and `review` were the last contexts the sidecar did not serve. Three operations of `ReviewQuery` take Views
(`threadsInDiff`, `anchorOf`, `pendingInDiff`): the shell hands back what each read last answered so that `review`
can join them. ADR 0050 gives one method per operation with the Payload its params and the View its result, and
rejects DTOs that exist only in the sidecar. `ReviewQuery.subscribe` takes two listeners, which one `docs/changed`-style
notification cannot carry.

## Decision

- **An operation that joins Views takes one Payload.** `threadsInDiff`, `anchorOf` and `pendingInDiff` take
  `ThreadsInDiffPayload`, `CommentAnchorPayload` and `PendingCommentsInDiffPayload` in `review.infrastructure.primary`, and the
  shell passes the same. The Java operation and the protocol method have one shape, and the sidecar declares no type
  of its own. Rejected: separate arguments in Java and a sidecar-side record, which is the DTO ADR 0050 refuses.
- **A Payload may hold a sealed type, read by its `kind`.** The writer leads a sealed type's variant with `kind` and
  the schema states it as `oneOf` with that discriminator, so the reader reads it the same way: `SealedViewDeserializer`
  picks the variant whose `kind` it is, and a `kind` no variant has is an invalid param. Where the declared type is
  the variant itself, its `kind` is checked the same way, and a param the schema does not state, or a null in a list,
  is refused as the schema's `additionalProperties: false` says. This closes the `deferred:`
  guard of ADR 0074 that refused a sealed Payload component.
- **`review/watch` serves `ReviewQuery.subscribe`.** It answers `checking()`'s `PullRequestsView`, then opens the
  subscription once per project root and pushes `review/changed` (`PullRequestsView`) and `review/mergedChanged`
  (`MergedPullRequestsView`), the two listeners of the one subscription. `docs/watch` does the same for
  `DocsQuery.subscribe` and pushes `docs/changed`. As `session/watch`, a watch that failed to open is forgotten before
  another of the root is let in, and the notifications are queued after the answer.
- **A repeat `review/watch` asks for a fetch.** `review` keeps no last fetch to answer it with, so a reloaded page
  would show `checking` until the next timed one; the sidecar calls `ReviewQuery.refresh` instead, one `gh` fetch per
  reload. Rejected: `review` keeping its last pull requests, an application change for one caller.
- **`docs/watch` and `review/watch` serve one project root.** `DocsWatchRepository` and `PullRequestWatchRepository`
  each hold one project, so a second root would take the first one's watcher over silently. The first root watched
  claims it and a watch of another is an invalid param; a watch that failed to open releases it. A sidecar runs in one
  project, so nothing reaches this today. Rejected: an adapter per root, wiring for a case nothing reaches.
- **`review/refresh` is a request that answers null.** `ReviewQuery.refresh` returns nothing, and the frontend
  wants to know the fetch was asked for, unlike a keystroke; the pull requests then arrive as `review/changed`. The
  schema states a result of `Void` as `null`.
- **A write GitHub refuses is a result.** `review/comment`, `reply`, `edit`, `delete` and `submit` answer
  `PendingReviewWriteView`, `RefusedWriteView` included; so is a refused anchor. A JSON-RPC error stays for a call
  that was not understood. `review/comment` does not check its anchor against the diff: GitHub stays the
  enforcer of a cross-hunk range, as it is for the shell, which always asks `review/anchorOf` first.
- **A method is declared once.** A method answered by one operation is a `ServedMethod`, its name, Payload, View and
  the operation as an unbound reference (`ReviewQuery::diffOf`); an input is a `ServedInput`. The schema reads the
  list without an adapter being built, and the sidecar binds the same list to the adapter it serves, so adding a method
  is one line rather than a constant, a schema entry and a handler.

## Consequences

- `ProtocolCoverageTest` lists `docs/subscribe` and `review/subscribe` as served by a watch.
- `review/watch` answers `checking()`, not the last fetch; on a repeat watch the pull requests follow within one
  `gh` fetch as `review/changed`.
- A frontend that watches two projects needs a sidecar per project, or an adapter per root (`deferred:` until one does).
- A frontend that comments without asking `review/anchorOf` first sees GitHub's refusal for a cross-hunk range rather
  than the domain's reason (`deferred:` on `review/comment`).
- A join's Payload carries the whole diff back, so the frontend sends it again on every range mark and rebuild. It
  keeps `review` holding no reading of its own and the join in the domain; in-process it costs nothing.
- The protocol test stubs `gh` for one read (`review/diffOf`) and one refused write (`review/submit`).
