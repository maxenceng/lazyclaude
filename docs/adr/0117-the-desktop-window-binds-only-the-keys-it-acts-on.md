# 117. The desktop window binds only the keys it acts on

Date: 2026-11-19

## Status

Accepted. Amends ADR 0058: the window no longer takes every key in the TUI's key table, only the ones something in the
window acts on; the rest of ADR 0058 stands. Builds on ADR 0038 (removing a ticket's worktree).

## Context

ADR 0058 gave the window the TUI's whole key table, each key prevented outside a pane. Three of them, `g`, `G` and `d`,
had nothing acting on them, so pressing one did nothing, silently, and the frontend architecture test allow-listed them
(#206). `g` and `G` have an obvious meaning on the one pane that scrolls without a cursor, the document. `d` removes a
ticket's worktree in the TUI (ADR 0038), a write, and the sidecar protocol has no method that reads or removes a worktree.

## Decision

- **A key is bound in the window only once something there acts on it.** The architecture test's allow-list of bound
  but unhandled actions is empty, and a key added to the table without a handler fails it.
- **`g` and `G` scroll the focused document to its top and its bottom,** as they scroll the TUI's main view. On every
  other pane they do nothing: the TUI does not move a list cursor on them either. The document key bar names them; its
  scrollbar is the visible control, so no button is added.
- **`d` is not bound in the window,** and `Action.RemoveWorktree` is gone from it. Removing a worktree stays the TUI's
  until the protocol carries a worktree read and remove, a ticket of its own (#236). Rejected: keeping `d` bound to a
  refusal, which is a key bar entry for an action that never succeeds.

## Consequences

- `d` reaches the webview unprevented outside a pane. The webview does nothing with a plain letter.
- When the window gains worktree removal, `d` comes back with its handler and a button, and this ADR's third point is
  superseded.
- The TUI's `g`, `G` and `d` are unchanged.
