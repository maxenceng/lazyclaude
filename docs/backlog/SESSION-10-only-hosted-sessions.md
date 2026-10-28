---
id: SESSION-10
status: in-review
context: session
type: feature
tracker_id: 93
created: 2026-10-26
---

# Only hosted sessions: outside sessions, Elsewhere, the kitty and tmux jumps and the transcript view go

## Acceptance criteria

- [ ] The Sessions panel's live block lists only the sessions lazyclaude hosts; a `claude` running in another terminal
      in the project does not appear. Past sessions are still found as today until the launch log replaces them.
- [ ] The left column has no Elsewhere panel, and a session running in another project shows nowhere.
- [ ] `o` does nothing anywhere and is gone from the key bar; no `kitten` or `tmux` process is run, and no prompt
      offers to edit `kitty.conf`.
- [ ] Selecting a past session shows its detail card in the main view (Design direction): title, first prompt, linked
      tickets with their last verb, directory, started and last active. Nothing tails its transcript.
- [ ] `enter` on a past session still resumes it, in its ticket's worktree when it is linked, as today.
- [ ] A past session whose transcript cannot be read shows its card with the reason in place of the facts it lacks,
      and still resumes.
- [ ] The glossary loses *Outside session*, *Jump*, *Elsewhere*, *Transcript view* and *Residence*, the context map's
      `session` row loses the outside sessions, and CLAUDE.md's hard rule 4 no longer names `tmux`.

## Model decision

A cut in `session` and the shell, no new aggregate (ADR 0047).

- Domain: `Residence` (`InKitty`, `InTmux`, `Nowhere`), `JumpManager`, `TerminalJumpPort`, `KittyConfig*`,
  `RemoteControlConfig` and its shapes, `ProcessEnvironmentPort`, `ElsewhereGroup` and the machine-wide grouping all go.
  Only the live sessions whose pid lazyclaude hosts count. Until SESSION-11 reads only those pids' records, the filter is
  `AvailableSessions.hosting`, applied by `SessionApplicationService` to the listing, the notifications, the worktree
  check and the verb preselection, and `SessionManager` still locates every live session it is handed. How their state is
  read changes in SESSION-11, how past sessions are found in SESSION-12.
- The transcript entries (`TranscriptEntry` and its prompt/reply/tool-call shapes, `TranscriptManager`,
  `TranscriptReadPort`) go with the transcript view; the facts a past row already carries are what the card shows.
- Secondary: `TerminalJumpRepository`, `KittyConfigRepository`, `ProcessEnvironmentRepository`,
  `TranscriptReadRepository` go.
- Primary: `OutsideSessionCommand`, `JumpPayload`, the `Elsewhere*View`s, `TranscriptQuery` shapes and the remote-control
  views go.
- Shell: `ElsewhereRows`, `ElsewhereRow`, `ElsewhereHeaderRow`, `ElsewhereSessionRow`, `TranscriptPane`,
  `TranscriptSubject` and `RemoteControlPromptText` go; a past-session card replaces the transcript pane.
- ADR 0008's left column becomes three panels; note it on that ADR's Status line.

## Design direction

Left column: Board, Sessions, Docs. The main view, a past session selected:

```
┌ Session ───────────────────────────────────────────────┐
│ Refine the zoom keys                                   │
│ › /ticket start PROJ-1                                 │
│                                                        │
│ PROJ-1  start                                          │
│ ~/projects/lazyclaude.worktrees/PROJ-1-zoom            │
│ started 2026-10-25 14:02 · last active 3 h ago         │
└────────────────────────────────────────────────────────┘
 enter resume   alt+h/j/k/l focus   alt+m main view   alt+z zoom
```

A title absent from the transcript shows the first prompt as the heading and omits the `›` line; an unlinked session
omits the ticket line.

## Glossary impact

Removes *Outside session*, *Jump*, *Elsewhere*, *Transcript view*, *Residence* and the `tmux` label. *Place* stays only
if the Sessions panel still groups by it; otherwise it goes too. Adds *Session card*: the main view's facts for the
past session selected in the Sessions panel.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map. Decided in [LAZY-25](LAZY-25-what-session-keeps.md),
recorded in ADR 0047. First of the phase: nothing else is ported, given mouse support or documented before it is cut.
