# 80. The desktop frontend reads its copy through i18n

Date: 2026-11-02

## Status

Accepted. Amends ADR 0057, whose `ddd-frontend` split dropped i18n.

## Context

ADR 0057 dropped the `ddd-frontend` skill's i18n because lazyclaude is English only. The review of the first screen
(PR #172) asked for it anyway: copy written inline in components is scattered, and every screen added after the Board
would have to be retrofitted when a catalogue arrives.

## Decision

- **`react-i18next` over `i18next`, both pinned exactly**, initialised synchronously from bundled JSON in
  `src/i18n/i18n.ts`, imported once by `main.tsx` and once by the test setup. Keys are typed from the English resources
  by `i18next.d.ts`, so an unknown key fails `tsc`.
- **One namespace per context, `designSystem` for the primitives' own words, `common` for the composition side**
  (`App.tsx`, `connection/`, the key bar). `src/i18n/` is a shared folder; the design system may import it and nothing
  else outside itself.
- **English only.** No `fr`, no parity test and no language picker, unlike the skill's template.
- **ESLint refuses literal copy**: JSX text, a literal or template as a JSX child or a conditional's branch, and a
  literal on `title`, `label`, `alt`, `placeholder` and the readable `aria-*`. Tests are exempt.
- A sidecar's own words — a refusal's `reason`, a JSON-RPC `error`'s `message` — are shown as sent, never looked up.

## Consequences

- A second language is new locale files, a parity test and a picker, with no component touched.
- Deferred: a literal passed to a component prop typed `string` other than the readable attributes passes the lint —
  close it with a type-aware `no-literal-copy` rule when one slips through review.
- Deferred: `SidecarUnavailableError`'s message is the client's own English shown in an `Alert` — close it when the
  fault state carries a cause instead of a message.
