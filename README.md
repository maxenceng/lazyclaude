# lazyclaude

A terminal IDE for [Claude Code](https://claude.com/claude-code): a project's
GitHub Issues as a board of tickets, and from a ticket, a jump into a live Claude Code session or a new
one.

Built on the [claude-tools](https://github.com/maxenceng/claude-tools) harness — the backlog
format, `/ticket` and the guards are its — in plain Java 25 with
[TamboUI](https://tamboui.dev).

## Run

```bash
make doctor   # JDK 25 present?
make run      # build the jar and open the TUI; q quits
```

## Develop

```bash
make test     # tests, architecture rules included
make verify   # everything CI runs, in order
```

Work goes through the backlog: `/ticket new`, `/ticket refine`, `/ticket start`. See
`CLAUDE.md` for the rules the build enforces and `docs/adr/` for why.
