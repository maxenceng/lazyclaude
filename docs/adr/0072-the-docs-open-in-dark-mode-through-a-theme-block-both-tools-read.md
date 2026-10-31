# 72. The docs open in dark mode, through a theme block both tools read

Date: 2026-10-31

## Status

Accepted. Builds on ADR 0069 (`mkdocs.yml` holds only what both Zensical and MkDocs 1.x with Material read).

## Context

The site opened in the light scheme, with no way to switch. A dark default is a `theme.palette` block, and ADR 0069
asks that block to stay readable by MkDocs 1.x with Material, the fallback while Zensical is 0.x. The two tools
disagree on what the block may hold: MkDocs refuses a `theme` with no `name`, Zensical refuses `name: mkdocs`, and
Material ships no Lucide icon, which is Zensical's own icon set.

## Decision

- **The palette lists `slate` first, then `default`, each with a toggle.** The first entry is the scheme a visitor
  gets, and the toggle lets them switch to light. Rejected: `slate` alone, which takes the choice away.
- **The theme is named `material`.** It is the only name both tools accept.
- **The toggle icons are `material/weather-sunny` and `material/weather-night`.** Both tools ship them. Rejected:
  `lucide/sun` and `lucide/moon`, which fail the MkDocs build with a missing template.

## Consequences

- The fallback needs `mkdocs-material` installed, which ADR 0069 already assumed.
- An icon added to `mkdocs.yml` later has to exist in Material's set as well as Zensical's.
