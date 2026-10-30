# What reading every past session costs at startup

Researched 2026-10-10 for [LAZY-16](https://github.com/maxenceng/lazyclaude/issues/95),
against Claude Code 2.1.294's transcripts on this machine (CachyOS, kernel 7.2.9, 16 cores,
23 GB RAM, NVMe, `/home` on btrfs with `compress=zstd:1`).

## Question

What does lazyclaude pay at startup to find every past session and its link, how does that
grow to thousands of transcripts, and what makes it cheap? Past sessions are found by reading
every `~/.claude/projects/*/*.jsonl` and matching each line's `cwd`; a session is linked to a
ticket by its first `/ticket <verb> <ID>`, then `pr-link`, then any `gitBranch`
([session-discovery](session-discovery.md), [session-ticket-link](session-ticket-link.md)).

**Short answer.** A naive full scan is already over budget today: 375 transcripts, 205 MB,
0.36–0.39 s cold and 0.17 s warm with a line prefilter, 0.8–1 s with a JSON parse per line.
It grows linearly with bytes, so a few thousand transcripts cost seconds. The fix is a cache
keyed on path + size + mtime: the stat pass that validates it takes about 1.5 ms for 375
files and the cache is about 365 bytes per session. Paint the first frame from the cache
and the live registry, and rescan only new or changed files in the background.

## Sources

- **[DOC]** Claude Code docs, *.claude directory*:
  https://code.claude.com/docs/en/claude-directory. Transcripts are deleted after
  `cleanupPeriodDays`, default 30 (quoted in [session-discovery](session-discovery.md)).
- **[O]** Measurements on this machine, read-only, against the 375 top-level
  `~/.claude/projects/*/*.jsonl` (subagent transcripts under `<session>/subagents/` are not
  sessions and were excluded). Throwaway scripts in Python 3.14 and Java 25 (single-file
  source launch), kept outside the repo. Only aggregates were recorded; no message content.

## How "cold" was approximated

Dropping the page cache system-wide needs root. Instead, before each cold run the script
called `posix_fadvise(POSIX_FADV_DONTNEED)` on every transcript, which evicts that file's
clean cached pages without changing it. It worked: a raw read of all 205 MB took 23 ms warm
and 200–345 ms after eviction. It does not evict the dentry and inode caches, so the stat
pass was never measured truly cold, and a first start after boot would be somewhat slower
than the cold numbers below. Cold reads here also include zstd decompression by btrfs.

## Findings [O]

### Size

| | |
|---|---|
| Project directories | 10 |
| Top-level transcripts | 375 |
| Total | 204.7 MB |
| Largest | 4.82 MB |
| Median / mean / p90 | 372 KB / 546 KB / 1.14 MB |
| Oldest mtime | under 30 days: retention caps the count |

375 sessions in 30 days is about 12 a day. With the default retention the count stays in the
hundreds; thousands means a raised `cleanupPeriodDays` or much heavier use [DOC].

### Full-scan wall time (375 files, 205 MB)

| Pass | Cold | Warm |
|---|---|---|
| Raw read, no parsing (I/O floor) | 200–345 ms | 23 ms |
| Python, `json.loads` on every line | 980 ms | 764 ms |
| Python, byte prefilter, parse only matching lines | 361–390 ms | 172 ms |
| Java 25, whole file to `String`, `indexOf` per field | 553 ms (also JIT-cold) | 225–305 ms |
| Head 64 KB + tail 64 KB per file, Python | 280 ms | 78 ms |
| Stat pass only (list + size + mtime), Python / Java | n/a (dentries warm) | 1.2 ms / 1.5 ms |

Parsing dominates warm; a byte prefilter (look for `"type":"ai-title"`,
`"type":"pr-link"`, `<command-args>`, `"gitBranch":"` before parsing a line) is what makes a
full scan tolerable at all. Cold, per-file read latency dominates: reading only 16 KB of
each file still took 144 ms, so a partial read barely helps a cold start.

### Where the fields sit

First occurrence, over the files that have the field:

| Field | Present | Byte offset p50 / p90 / max | Line p50 / p90 / max | Fraction of file p50 / p90 |
|---|---|---|---|---|
| `cwd`, `sessionId`, first `gitBranch` | 375 | 0.9 K / 53 K / 403 K | 2 / 4 / 5 | 0.01 / 0.18 |
| first `/ticket <verb> <ID>` | 130 | 16 K / 16.5 K / 306 K | 9 / 13 / 76 | 0.02 / 0.04 |
| `ai-title` | 244 | 108 K / 548 K / 3.5 M | 17 / 112 / 1186 | 0.35 / 0.78 |
| `pr-link` | 89 | 722 K / 1.66 M / 2.8 M | 206 / 557 / 801 | 0.93 / 0.98 |

- `cwd`, `sessionId`, the first branch and the `/ticket` invocation sit at the start. The
  first lines can be large (a file-history snapshot), hence the 53 K p90 at line 4.
- `ai-title` and `pr-link` are rewritten near the end. 242 of 244 titled files have more
  than one `ai-title` line, all identical within a file; the last one is always within 38 KB
  of the end. 66 of 89 files repeat `pr-link`; the last one is always within 64 KB of the
  end.
- Head 64 KB + tail 64 KB finds `cwd` 375/375, title 244/244, `pr-link` 89/89 and the
  `/ticket` invocation 128/130.
- What it cannot find is **every** `gitBranch`: 65 sessions have more than one distinct
  branch, and for 43 of them the first branch misses a ticket id seen later. The weak link
  rule ("any `gitBranch`") needs the whole file.

So a scan can stop early for everything except the full branch set, but that only halves
the warm cost and hardly changes the cold one.

### A cache keyed on path + size + mtime

- Validation is one directory listing plus one `stat` per file: 1.2–1.5 ms for 375.
- One entry (path, size, mtime, `cwd`, `sessionId`, branches, title, first `/ticket`,
  `pr-link`) is about 365 bytes as JSON: 137 KB today, parsed in 0.6 ms.
- 350 of 375 transcripts were untouched for over a day, so on a warm start nearly every
  entry is a hit; the misses are the few live or recent sessions.
- Transcripts look append-only, so a grown file could be read from its old size onward.
  That is not documented; a safe first version rescans the whole changed file.

## Extrapolation

Costs scale with bytes (about 0.85 ms/MB warm and 1.8–2.7 ms/MB cold for the prefiltered
scans) and the stat pass with file count. At this machine's mean of 546 KB per transcript:

| Transcripts | Bytes | Full scan warm | Full scan cold | Stat pass | Cache |
|---|---|---|---|---|---|
| 375 (today) | 205 MB | 0.17–0.3 s | 0.36–0.55 s | ~1.5 ms | 137 KB |
| 1,000 | 0.55 GB | ~0.5 s | ~1–1.5 s | ~4 ms | ~0.4 MB |
| 3,000 | 1.6 GB | ~1.4 s | ~3–4.5 s | ~12 ms | ~1.1 MB |
| 5,000 | 2.7 GB | ~2.3 s | ~5–7 s | ~20 ms | ~1.8 MB |

With a budget of about 100 ms of startup for past sessions, a blocking full scan stops being
acceptable at roughly 100–150 MB, about 200–300 transcripts warm and fewer cold: this machine
is already past it. A JSON parse per line crosses it at around 50 transcripts.

## Options

| Fix | Warm start | Cold start | Catch |
|---|---|---|---|
| Early stop (head + tail 64 KB) | about 2× cheaper | little better | Misses later branches; still linear in files |
| Lazy per-project scan | none | none | A session's directory is its launch directory, not its project, so every directory has to be read anyway |
| mtime cache | ~stat pass + cache load: a few ms, even at 5,000 | first run pays a full scan once | Needs a cache file under `$XDG_CACHE_HOME`; invalidate on size or mtime change |
| Background scan after first frame | first frame immediate | first frame immediate | The list fills in; sessions appear or relink a moment later |

## Answer

Use the cache and the background scan together. Draw the first frame from the live
registry plus the cached past sessions. Then, off the UI thread, stat every transcript,
rescan only the new or changed files with the byte prefilter, and update the screen as
entries arrive. A first run with no cache shows live sessions at once and fills in past
ones over a second or so. The early stop is optional and not worth the rule complexity,
because the weak branch link needs the whole file anyway.
