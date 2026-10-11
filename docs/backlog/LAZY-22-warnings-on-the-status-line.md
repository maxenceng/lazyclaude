---
id: LAZY-22
status: done
context: cross-context
type: feature
tracker_id: 91
created: 2026-10-11
---

# A warning raised after start-up shows on the screen instead of printing over the TUI

## Acceptance criteria

- [x] A warning raised while the screen is up shows on the status line, right-aligned; nothing is printed over the screen.
- [x] A warning raised before the screen opens — the docs watch, the session fallback, the sessions watch — shows on the first frame.
- [x] A warning stays until lazyclaude quits; there is no key or timer that clears it.
- [x] A second warning shows as `⚠ 2 · <newest>`.
- [x] A warning longer than the room the status line has left is cut with `…`, never wrapped.
- [x] With no warning, the status line is unchanged.
- [x] Nothing lazyclaude writes reaches stderr while the screen is up.
- [x] The board-read failure at start-up still prints and exits: it is a refusal to start, not a warning.

## Model decision

No context's aggregate changes; the shared kernel owns warnings, since the shell may not be depended on and the warnings come from more than one context.

- `shared.domain` gets a `Warning` value (its text) and a `WarningPort` (`raise(Warning)`).
- `shared.infrastructure.secondary.WarningRepository` implements it in memory: it keeps every warning raised and replays the held ones to a subscriber, which is how a warning raised before the screen exists reaches the first frame.
- `shared.infrastructure.primary.WarningQuery.subscribe` hands the shell a `WarningView` (newest warning and count), applied on the render thread the way docs and sessions views are.
- `DocsWatchRepository`, `LiveSessionRepository` and `SessionWatchRepository` take a `WarningPort` in place of their `PrintStream`.

Rejected: each context's view carrying its warning (the sessions-watch warning has no view to ride on, and the session fallback would have to be threaded through the domain); the shell handing a bare `Consumer<String>` down (bypasses the Query/View roles of ADR 0012).

An ADR records that warnings go through the kernel and never to stderr, and that the shared kernel gains a primary adapter (extends ADR 0017).

## Design direction

The warning sits right-aligned on the status line, `⚠` and its text in the warning colour (yellow). The left side stays free for the session markers. No new key.

```
● 1 waiting  2 your turn            ⚠ docs: cannot watch docs/ (…); checking every 5 s
┌ Board ─────────┐┌ Main view ───────────────────────────────┐
│ …              ││ …                                         │
└────────────────┘└───────────────────────────────────────────┘
j/k move  h/l panel  ? keys
```

## Glossary impact

- adds **Warning** (`shared`): a degraded mode lazyclaude carries on in, named once; shown on the status line until quit.
- adds **Status line** (`shell`): the top row of the screen; markers on the left, the warning on the right. Already used by the layout tickets, missing from the glossary.

## Notes

Raised in the review of [SESSION-1](SESSION-1-live-sessions.md). `LiveSessionRepository`'s fallback warning and `DocsWatchRepository`'s watch warning both print to stderr. One that first fires after start-up lands on top of the drawn screen. The fix is suggested to sit in the shell, as a status-line message both warnings go through.

Refining found a third warning the draft missed, `SessionWatchRepository`'s "cannot watch sessions", which fires only at start-up; it is in scope. The status line is shared with the session markers planned by the layout tickets, so the warning takes the right side rather than the whole line.

Closed after [#12](https://github.com/maxenceng/lazyclaude/pull/12). Review changed the Model decision in one place. ADR 0022 records it, and the section above is left as decided:

- The three adapters hold no `WarningPort`. `ports_are_held_only_by_the_domain` forbids a port field in a secondary adapter. Each one reports its fallback through a callback on its context's port (`WatchFallback`, `SourceFallback`), and the `docs` and `session` managers raise the warning. Each manager names a degraded mode once, through a kernel `WarnOnce`.
- Review widened the scope by one warning: a thread that ends on an uncaught failure raises one, through `WarningCommand`. The `main` thread still prints, since the screen is gone by then.
- `SessionView` now carries a `Path` rather than the kernel's `WorkingDirectory`. The shell-reach rule now covers kernels too.
- Moving the registry-or-`claude agents` choice into `SessionManager` was rejected for now; a `deferred:` note on `LiveSessionRepository` records it.
