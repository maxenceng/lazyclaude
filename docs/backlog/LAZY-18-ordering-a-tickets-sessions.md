---
id: LAZY-18
status: done
context: cross-context
type: decision
created: 2026-10-10
blocked_by: [LAZY-1, LAZY-3, LAZY-10]
---

# In what order are a ticket's several sessions listed?

## Question

In what order does the Sessions panel list a ticket's several sessions, and which one is selected first?

Several sessions per ticket is the norm (LAZY-3: 28 of 30 tickets in next-suggestions). They are rows in the Sessions panel (LAZY-10), and they are hosted, outside or past, with states busy, your turn, waiting or starting. Order them by liveness (live before past), by recency, or by the `/ticket` verb they ran? Are past sessions grouped or collapsed? Is the weak link (LAZY-3) shown apart?

## Kind

grilling

## Resolution

**Live first, then past; stable orders, so rows don't jump under the cursor.** In
next-suggestions a ticket has 1 to 6 linked sessions (mostly 4–5). About 160 of 295 sessions
carry no `/ticket` args, so only the unlinked section needs folding.

- **Live block first** (hosted, outside, starting), ordered by start time, newest first.
  It is never ordered by state or by last activity, because either would reorder rows
  whenever a session changes state. Urgency is the marker's job, and `w`'s.
- **Past block after**, ordered by last activity (the transcript's last entry), newest
  first. For a past session that time is fixed, so the order is stable.
- **No folding under a ticket.** Every linked session is shown.
- **Unlinked section:** the same two blocks and orders. All live sessions are shown, then
  the 5 newest past ones, then a `… N older` row that expands in place. The cap is a count,
  not a time window, so the section is never empty after a week away.
- **Weak links** stay inline in the same order, with a dim `(branch)` suffix. There is no
  third section.
- **Selection on `enter` from a ticket:** the cursor lands on the session with the strongest
  marker (`!` > `?` > `•`). Ties go to the first row in the order above. With no session
  showing a marker, it lands on the `n new: /ticket <verb>` row (LAZY-9's preselected verb).
