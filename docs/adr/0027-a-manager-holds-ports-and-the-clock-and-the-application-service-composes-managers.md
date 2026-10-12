# 27. A manager holds ports and the clock, and the application service composes managers

Date: 2026-10-12

## Status

Accepted. Narrows ADR 0011's manager role. Follows next-suggestions' ADR 0077.

## Context

ADR 0011 gave each context a `*Manager` that calls `*Port`s, and `ArchitectureTest` failed the build when a manager
took another `*Manager` as a constructor parameter. Hosting sessions got around that rule without breaking it.
`SessionManager` needed to know which hosted sessions were open, so it took a `HostedSessionsSource` callback, and
`SessionApplicationService` wired `HostedSessionManager::open` into it. The rule only looked at parameter type names, so
it passed, even though one manager was reading another manager's state. Review rejected that.

## Decision

A class in `..domain..` whose name ends `Manager` takes only `*Port`s and `java.time.Clock` in its constructor. It
computes everything else itself. A value object built from the clock, such as `SessionClock`, is built inside.

When one manager's use case needs another manager's answer, the `*ApplicationService` calls both and passes the first
answer to the second. For hosted sessions, `SessionManager` reads the live and past sessions, and
`SessionApplicationService` lays `HostedSessionManager.open()` over each read with `Sessions.hosting`, both on a direct
read and on every delivery to a subscriber.

`ArchitectureTest.managers_take_only_ports_and_the_clock` enforces this. It was watched to fail on the
`HostedSessionsSource` wiring. The older rule against `*Manager` parameters stays, because it also covers domain
classes that are not managers.

## Consequences

- A callback, supplier or functional interface handed to a manager now fails the build, whatever it is called. The only
  way for a manager to learn another manager's state is through the application service.
- Composing in the application service puts an ordering duty on it: every path that hands out `Sessions` must apply
  `hosting`. `sessionsFor` and `subscribe` are the only two paths today, and `SessionApplicationServiceTest` covers both.
- A manager that needs a collaborator that is neither a port nor the clock has to make it a port, or get an ADR to widen
  the rule.
