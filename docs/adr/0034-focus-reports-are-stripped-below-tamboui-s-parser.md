# 34. Focus reports are stripped below TamboUI's parser

Date: 2026-10-17

## Status

Accepted. Narrows the shell's relation to TamboUI: it now supplies the backend. To be reversed once TamboUI decodes focus.

## Context

Suppressing a notification while the terminal has focus needs focus events (`CSI ?1004h`, then `ESC [ I` in and
`ESC [ O` out). TamboUI 0.5.0 does not pass them on. `EventParser` sends `ESC [ I` and `ESC [ O` to `parseExtendedCSI`,
which takes the letter as a parameter, reads the next input byte as the terminator and returns `KeyCode.UNKNOWN`: focus in
and focus out are indistinguishable, and the keystroke that follows is eaten.

## Decision

The shell hands TamboUI a decorating `Backend` through `TuiConfig.builder().backend(...)`. `FocusReportingBackend`
delegates everything and sits between the terminal and the parser:

- in `read` and `peek` it recognises `ESC [ I` and `ESC [ O`, takes them out of the input so `EventParser` never sees them,
  and tells the shell's `TerminalFocus`;
- every other byte reaches the parser unchanged: a lone `ESC` (a lookahead that finds nothing returns it), `ESC [ A`, an
  `ESC [ 1;5A` with parameters, and an Alt chord. A byte it looked at and does not own is pushed back, in order;
- it writes `ESC[?1004h` on entering the alternate screen and `ESC[?1004l` on leaving it, so the terminal stops reporting
  when the screen is given back.

`TerminalFocus` counts the terminal as focused until a focus-out arrives. It lives in `shell`, the one place that may
touch the terminal. The shell works out the watched session on the render thread and tells `session` through
`WatchedSessionCommand`, off it.

## Consequences

- The wrapper must forward every `Backend` method a real backend overrides, defaults included, or that capability
  silently falls back to the interface's no-op. A new `Backend` method in a TamboUI upgrade is not forwarded until added.
- A lone `ESC` costs a second 50 ms lookahead before the parser's own, so it takes about 100 ms to arrive.
- An `ESC` typed immediately before a focus report leaves the report's `[I` to be read as typed characters.
- The wrapper carries a `// deferred:` comment: remove it, and `ESC[?1004h`, when TamboUI decodes focus events itself.
- `PullRequestWatchRepository` could pause its timer while the terminal is unfocused; it does not, and says so.
