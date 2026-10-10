# 13. Tests build their values from fixtures and mock ports with Mockito

Date: 2026-10-10

## Status

Accepted

## Context

The first tests each carried private helpers that built tickets, boards and files with their own
literals: `ticket("A-1", "draft", "Idea")` in one class, a different `ticket(...)` in the next.
Two tests needing the same value retyped it, and nothing failed when they drifted apart. Ports
were stubbed with lambdas written inline, so a stub that ignored its argument looked the same as
one that checked it. The author's other projects and the claude-tools template already settled
both questions.

## Decision

- **A `*Test` class holds tests only.** A helper it needs is a static method on the
  `<Type>Fixture` beside the type in the test tree, named for the state it builds: `ticket()`,
  `bareTicket()`, `ticketViewBuilder()`. `TestConventionsTest` fails on any non-private method
  in a `*Test` class, ArchUnit rule holders excepted.
- **Ports are mocked with Mockito** (`@Mock`, `@InjectMocks`, `when(...)`), stubbed with the
  exact argument the code under test should pass. Real adapters are exercised against a real
  filesystem and real `git`, through `BacklogFixture` and `GitFixture`.
- **Expected values come from the same fixtures**, so a test compares whole entries or views
  instead of picking at fields.

Mockito is loaded as a Java agent by Surefire, as its documentation requires on current JDKs.

## Consequences

- A new domain type comes with its fixture; a test that needs a new state adds a named method
  there rather than a literal in the test.
- Fixtures are shared across packages, so changing one changes every test that reads it — the
  point, but it means a fixture edit is reviewed like code.
