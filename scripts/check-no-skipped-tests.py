#!/usr/bin/env python3
"""Fail when the last test run skipped anything.

A test that needs something absent — a `claude` binary, a tmux server — tends to grow an
assumption that skips it, and the run still reports BUILD SUCCESS. A green run then says
nothing about the paths only the real tool reaches. Wired into `verify` rather than `ci`, so
a contributor missing the tool can still run the rest of the suite.

Read with a regex rather than an XML parser: the input is our own build output, and the
attributes wanted sit on one known element, so a parser would add a dependency and an
entity-expansion surface for nothing.
"""
import glob
import re
import sys

REPORTS = "target/surefire-reports/*.xml"

SUITE = re.compile(
    r"<testsuite\b[^>]*\bname=\"(?P<name>[^\"]*)\"[^>]*", re.DOTALL)
ATTR = r'\b{}="(\d+)"'


def main():
    reports = glob.glob(REPORTS)
    if not reports:
        print(f"no surefire reports under {REPORTS} — run the tests first")
        return 1

    skipped = []
    for report in reports:
        with open(report, encoding="utf-8", errors="replace") as handle:
            head = handle.read(4096)

        suite = SUITE.search(head)
        if not suite:
            continue

        counts = {}
        for attribute in ("skipped", "tests"):
            found = re.search(ATTR.format(attribute), suite.group(0))
            counts[attribute] = int(found.group(1)) if found else 0

        if counts["skipped"]:
            skipped.append((suite.group("name"), counts["skipped"], counts["tests"]))

    if not skipped:
        return 0

    total = sum(count for _, count, _ in skipped)
    print(f"{total} test(s) skipped — the suite did not prove what it looks like it proved:")
    for name, count, tests in sorted(skipped):
        print(f"  {name}: {count} of {tests}")
    print("\nA skip usually means a tool the test needs is missing. Run make doctor.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
