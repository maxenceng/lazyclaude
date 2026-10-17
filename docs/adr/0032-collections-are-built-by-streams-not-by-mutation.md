# 32. Collections are built by streams, not by mutation

Date: 2026-10-17

## Status

Accepted. Applies ADR 0025 to building a collection by adding to it.

## Context

Review asked for a stream where a list was filled by `forEach(segment -> segments.add(...))`, then said why: mutations
are to be avoided. The rule against `for` loops did not cover it, since `forEach` with an `add`, or a set handed to a
method that adds to it, is the same loop under another name. The same shape had spread to the shell's row lists, the
key bar, the transcript scan and the transcript watch loop.

## Decision

PMD's `BuildCollectionsWithoutMutation` fails the build on, in `src/main`:

- a mutable collection (`ArrayList`, `HashSet`, `HashMap` and their kin) created inside a method;
- a `forEach` whose lambda or method reference calls `add`, `put`, `remove` or their bulk forms;
- a method that calls one of those on a collection it was handed as a parameter.

A collection is built by a stream, by `Stream.concat` and `flatMap` over the parts it is made of, or by reassigning an
immutable value, as `FoldedKeys.toggled` and `PendingTranscripts` do. The rule was watched to fail on 39 sites before
they were rewritten.

## Consequences

- State a field holds is not covered: a manager's open sessions, the warnings raised, the watcher's registrations,
  the transcript cache. Each is the object's state rather than a value being built, and can still be changed in place
  until review asks for one of them to be immutable.
- A loop that must throw a checked exception keeps its `for`, but reassigns nothing, as the transcript watch's
  `watchFolders` does. Folding a result over a loop needs a stream, so the transcript watch registers folders only
  there, and a folder created in its root re-states every transcript as an overflow does.
- A collection mutated through a local alias of a field, or through a method other than the ones named, is not caught.
