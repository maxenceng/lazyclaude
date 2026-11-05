# 88. The desktop window names every pattern at module level

Date: 2026-11-05

## Status

Accepted. Carries ADR 0044 (a string is named) to the frontend's regular expressions.

## Context

The Docs panel's review of `markdown.ts` asked "Magic string, what does this mean?" about the slug's character class,
written inline in a `.replace`. Its block patterns were already named constants (`FENCE`, `HEADING`), but six more
were written inline. ADR 0044 names a string in the Java code for the same reason. A pattern is no easier to read than a
string, so it is named too.

## Decision

- **A regular expression literal is the value of a module-level `const`**, whose name says what it matches
  (`NOT_IN_SLUG`, `TABLE_ROW`). A pattern a reader cannot tell from its name gets a doc comment saying what it matches.
- **ESLint enforces it** in `src/` outside tests: `no-restricted-syntax` refuses a regex literal anywhere other than the
  initialiser of a top-level or exported `const`. It was watched to fail on the six inline patterns in `markdown.ts`.

## Consequences

- Tests keep inline patterns: a `getByText(/…/)` matcher is the assertion itself, and naming it says nothing more.
- A pattern built at runtime with `new RegExp` is not checked. None exists yet.
