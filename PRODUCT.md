# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

One developer working on their own projects, the author. They type on AZERTY, in kitty, under Hyprland on an NVIDIA
card, on Linux x86-64 (ADR 0056), and keep most of a day's work inside Claude Code sessions. No other audience is
planned; tuning for this setup is allowed.

## Product Purpose

lazyclaude is a terminal IDE for Claude Code. It shows a project's GitHub Issues as a board of tickets and, from a
ticket, jumps into a live Claude Code session or starts a new one: the `/ticket` loop of the claude-tools harness on one
screen. Success is never leaving that screen to learn what a ticket is, which session is waiting on you, or what a
review says.

## Positioning

The real `claude` runs embedded on a pty beside the board (ADR 0007), so the sessions are not a chat imitation and the
tickets are not a copy: the issues are read through `gh` and the board's words are the backlog's words.

## Operating Context

- Two frontends over one backend: the TUI, in-process on TamboUI, and the desktop frontend, a Tauri window (React over
  xterm.js) driving `lazyclaude --sidecar` (ADR 0050, ADR 0057).
- The desktop frontend exists for what a terminal renders badly, with the same tool underneath: the same screens,
  keys and workflow as the TUI (ADR 0057), plus the mouse (ADR 0051), real selection and desktop notifications. The
  TUI stays first-class.
- The window is sometimes a Hyprland tile sharing the monitor with an editor or browser and sometimes full screen on
  its own workspace; every screen has to work from a half tile up.
- Keyboard first, lazygit-style: `j`/`k`, `h`/`l`, `enter`, `?`, `q`, with `alt+h/j/k/l/m/z/w` reserved inside every
  hosted session (ADR 0008, ADR 0058). No key needs AltGr or Shift on AZERTY.

## Capabilities and Constraints

- One screen: Board, Sessions and Docs panels on the left; a main view following the selection over a dock of hosted
  session tabs on the right; zoom for the status columns, the main view or the dock (ADR 0008).
- Read-only except two writes to GitHub: the pending review (ADR 0009) and capturing a draft ticket (ADR 0054). Files
  are edited in your own editor or through a session.
- Only the project's GitHub Issues, read through `gh`; Claude only through the `claude` CLI.
- Every failure is shown as a reason on screen (a refused or unavailable View, a warning on the status line), never
  dropped silently and never retried by itself.
- English only. Linux x86-64 only.
- Terms follow `docs/glossary.md`: ticket, status, board, session, never issue, card or chat.

## Brand Commitments

The name `lazyclaude`, lowercase, after lazygit, whose keys and panel layout it follows.

## Evidence on Hand

The running TUI (`make run`) is the reference for every desktop screen. No screenshots, users or testimonials exist;
none are to be invented.

## Product Principles

1. The TUI is the spec: a desktop screen that differs from it needs a reason.
2. A session is never hidden by the board, and the board never by a session.
3. Absence and failure are shown, with their reason, not papered over.
4. Every write is one the user asked for, and few writes exist.
5. Density over decoration: a tool used all day, by one person who knows it.
