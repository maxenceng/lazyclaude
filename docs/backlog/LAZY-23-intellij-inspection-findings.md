---
id: LAZY-23
status: in-review
context: cross-context
type: chore
tracker_id: 92
created: 2026-10-24
---

# Clear what IntelliJ's code inspection reports, and decide which of it the build should enforce

## Acceptance criteria

- [x] Every unused pattern binding (74, e.g. `case NoBody noBody ->`) is the unnamed `_`.
- [x] `Shell.idOf` and `RunStyle.has`, never called, are gone; `CommentBox` no longer calls the deprecated `character()`.
- [ ] The one-off findings are fixed: identical `catch` branches (`KittyConfigManager`, `SessionManager`), lambdas that are a method
      reference, inferable type arguments, the redundant cast, the two `Math.clamp` migrations, `Files.readString`, the duplicated
      `Path.of(xdgCacheHome)`, the pattern variable hiding a field in `BoardManager`, the single-case `switch`, the Javadoc blank
      line, the two always-inverted boolean methods, the two unused return values, the two always-same parameters.
- [ ] The 19 `DeconstructionCanBeUsed` sites use a record pattern.
- [ ] Genuine typos and grammar mistakes in comments are fixed; project words (`lazyclaude`, `gitdir`, `truecolor`), British
      spelling and domain verbs used as nouns ("the submit", "a write") are left.
- [x] No `Optional` is checked in one place and unwrapped in another: the method that checks hands the value on as a parameter
      (`removalPrompt.map(asked -> answerRemovalPrompt(asked, key))`, `ReviewKeys`' submit prompt likewise) — every check-then-get
      pair in `src/main`, not only the five IntelliJ flagged.
- [x] A watcher's service is one sealed state, `Watching(WatchService) | NotWatching(reason)`, replacing the paired `service` /
      `serviceFailure` optionals in `TreeWatcher`, `SessionWatchRepository` and `TranscriptWatchRepository`; each loop matches it
      once and its helpers take the `WatchService`.
- [x] The no-argument `orElseThrow()` sites (`TicketFrontmatter`, `PullRequestDiff`, `AvailableSessionsView`) take their value
      through `map` / `or` / `orElse` or a named exception instead.
- [x] `PullRequestWatchRepository` drains its request permits only when `tryAcquire` reports a request woke it.
- [x] `make verify` fails on any `javac -Xlint:all` warning (`-Werror`); whatever it reports beyond the IDE's list is fixed here.
- [x] `make verify` fails on PMD `IdenticalCatchBranches` and `LambdaCanBeMethodReference`.
- [x] `ArchitectureTest` fails on a call to `Optional.get()` or no-argument `Optional.orElseThrow()` in `src/main`.
- [x] `make verify` fails on an unused pattern binding if Checkstyle or PMD can be shown to catch one; if neither can, Notes says so
      and it stays a one-off fix.
- [x] Each new gate has been watched to fail on a planted case — a deprecated call, an identical catch, a lambda that is a method
      reference, an `Optional.get()` — and the PR says so.
- [x] Left showing in the IDE, unfixed: "constructor never used" on the Jackson-read `*Record`s, "enum never used" on `ModuleType`,
      `SameReturnValue` on the `||`-chained key handlers, `NullableProblems` on library overrides, `AutoCloseableResource` on
      `ToolkitRunner`, the 75 `Optional` fields and parameters, and the prose excluded above.

## Model decision

No domain behaviour changes and nothing new is modelled: build gates plus refactors in every module, hence
`cross-context`. The one new shape is the watchers' sealed `Watching | NotWatching`, an infrastructure state in `shared` and
`session` secondary adapters, following ADR 0012's absence-is-a-type rather than two optionals that must agree. Enforcement goes
through the existing gates — javac, PMD, ArchUnit — not IntelliJ or Qodana, so `make verify` stays the one gate (ADR 0025). One
ADR, in the same change, records the new gates and the rule that an `Optional` is unwrapped where it is checked.

## Design direction

n/a

## Glossary impact

none

## Notes

Raised while closing [REVIEW-3](REVIEW-3-writing-a-review.md): "IntelliJ code inspection shows a lot of issues." No findings
were captured. The headless run (`idea inspect`) refuses to start while the IDE is open, so refine either exports the results
from the IDE (*Code → Inspect Code*, then export) or runs it with the IDE closed. Spans every context; refine settles the prefix.

Refine read the IDE's export (*Code → Inspect Code*, production sources, 381 findings over 25 inspections). The headless run fails
even with the IDE closed: IntelliJ 2026.2.1's `inspect.sh` throws "Tools are not initialized" in `GlobalInspectionContextBase`.

Review settled two more on PR #41: the `serial` lint is off (`-Xlint:all,-serial`), as no exception is ever serialised and a
`serialVersionUID` on each said nothing; and `var` is banned in main and test sources, failed by `make var-check` (ADR 0045).
