---
id: LAZY-16
status: done
context: cross-context
type: decision
created: 2026-10-10
blocked_by: [LAZY-1, LAZY-2, LAZY-3]
---

# What does lazyclaude pay at startup to read every past session, and can it be cheaper?

## Question

What does it cost at startup to find every past session and its link, and how far does that grow?

LAZY-2 reads past sessions from transcripts in every `~/.claude/projects/*` folder, matched on each line's `cwd`. LAZY-3 links a session through the first `/ticket <verb> <ID>`, then `pr-link`, then `gitBranch`. Both need to read transcript content. Measure on this machine:
- how many transcripts there are, and their total and largest size;
- the wall time for a cold and a warm full scan that extracts `cwd`, `sessionId`, the `ai-title`, the first `/ticket` invocation, `pr-link` and `gitBranch`;
- how much of each file has to be read (do the link fields sit near the start?);
- whether a cache keyed on path + size + mtime would make a warm start cheap, and roughly how big it would be.

Extrapolate to "thousands of transcripts" and say where a naive full scan stops being acceptable for a TUI that should open in well under a second.

## Kind

research

## Resolution

**A blocking full scan is already too slow; cache each transcript's fields on path + size + mtime and scan only the misses after the first frame.**

- Today: 375 transcripts, 205 MB, largest 4.8 MB, median 372 KB. The 30-day retention keeps the count in the hundreds.
  - Full scan with a byte prefilter: 0.36–0.55 s cold, 0.17–0.3 s warm. Parsing every line as JSON: 0.8–1 s.
  - It grows with bytes: about 1.4 s warm and 3–4.5 s cold at 3,000 transcripts. It passes a ~100 ms budget at roughly 200–300 transcripts.
- Early stop helps little.
  - `cwd`, `sessionId`, the first branch and the first `/ticket` sit in the first few lines. `ai-title` and `pr-link` are rewritten within 64 KB of the end.
  - But "any `gitBranch`" needs the whole file (43 sessions gain a ticket id after their first branch). Head + tail only halves the warm cost, and cold cost is per-file latency.
- The cache makes a warm start cheap.
  - Validating it is one stat per file: 1.5 ms for 375, about 20 ms for 5,000.
  - About 365 bytes per session: 137 KB today, under 2 MB at 5,000.
- Draw the first frame from the live registry and the cache, then rescan new or changed files off the UI thread. A lazy per-project scan does not help, because a transcript's directory is not its project.

Findings and numbers: [docs/research/startup-cost-of-transcripts.md](../research/startup-cost-of-transcripts.md).
