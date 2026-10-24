# 45. The build fails on a compiler warning, and an Optional is unwrapped where it is checked

Date: 2026-10-24

## Status

Accepted. Applies ADR 0025 to what IntelliJ's code inspection reported and the build did not.

## Context

IntelliJ's inspection of the production sources reported 381 findings over 25 inspections. Most were one-offs, fixed in the same
change. Three kinds would come back unless a gate stopped them: a compiler warning, a lambda that is a method reference or a
`catch` that repeats another, and an `Optional` that one method checks and another unwraps with `get()`. `Optional.get()` and
`orElseThrow()` with no argument say nothing of what was absent, and the check that justified them can drift from the call.

## Decision

- **`javac -Xlint:all -Werror`** in the compiler plugin, for main and test sources, so a deprecated call, an unchecked cast, a raw type or an exception
  without a `serialVersionUID` fails `make verify`. Every warning it found is fixed rather than suppressed.
- **PMD `IdenticalCatchBranches` and `LambdaCanBeMethodReference`** in `config/pmd-rules.xml` at priority 1 (the plugin's `failurePriority`; the rules' own default of 3 would only warn), run by
  `make rules`. A merged
  `catch` of two exceptions whose common type is `DomainException` would make a manager depend on `error`, so a context's
  exceptions that are handled alike get a common base of their own, as `KittyConfigException` does. An exception from another
  module, which cannot share that base, is translated into the context's own at the call that receives it, as `SessionManager`
  turns the shared kernel's `ProjectUnavailableException` into `SessionsUnavailableException`.
- **`ArchitectureTest`**: `an_optional_is_never_unwrapped_unchecked` fails a call to `Optional.get()` or to `orElseThrow()` with
  no argument anywhere in `src/main`. An `Optional` is unwrapped where it is checked: the method that checks hands the value on
  as a parameter (`removalPrompt.map(asked -> answerRemovalPrompt(asked, key))`), or takes a default through `orElse`, or a named
  exception through `orElseThrow(Supplier)`. A prompt that falls through to the next is a chain of `or`.
- **A watcher's service is one sealed state**, `Watching(WatchService) | NotWatching(reason)`, in the shared kernel's secondary
  adapters, built by `WatchServiceState.of`. Each loop matches it once and its helpers take the `WatchService`, so a service and
  the reason it is missing cannot disagree.
- **An unused pattern binding** is the unnamed `_`, and Checkstyle `UnusedLocalVariable` in `config/checkstyle.xml` fails on one
  in `src/main`, as `case NoBody noBody -> "x"` or `if (o instanceof Foo foo) { return 1; }`. PMD `UnusedLocalVariable` and
  `UnusedAssignment` did not catch either, and Checkstyle `UnusedLambdaParameter` does not exist in 14.3.0. Checkstyle does not
  read `src/test`, so a binding there stays a one-off fix.

Each gate was watched to fail on a planted case before it was kept.

## Consequences

- Left showing in the IDE, deliberately: "constructor never used" on the Jackson-read `*Record`s, "enum never used" on
  `ModuleType`, `SameReturnValue` on the `||`-chained key handlers, `NullableProblems` on library overrides,
  `AutoCloseableResource` on `ToolkitRunner`, and the 75 `Optional` fields and parameters, which are how the shell holds a value
  that is sometimes absent. `SameParameterValue` on `TranscriptCache`'s version stays too: it is the wire shape Jackson reads back.
- Enforcement stays in `make verify`. IntelliJ and Qodana are not run in CI, so a finding they alone report is not a gate.
- `-Xlint:all` makes a new compiler lint a build failure after a JDK upgrade; the fix is the code, or a narrowly scoped
  `@SuppressWarnings` with its reason beside it.
