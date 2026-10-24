#!/usr/bin/env python3
"""Fail on `var` used as a type anywhere under src/main/java or src/test/java.

A type written out is read without an IDE; `var` hands the reader's job to the compiler. A
local declaration, a resource, a lambda parameter and a record pattern component all count.

PMD's UseExplicitTypes sees local declarations only, not a record pattern's `var`, and PMD
does not read src/test here - ADR 0045.
"""
import os
import re
import sys

# CHECK_ROOT is how guard-fixtures.py points this at a fixture tree; nothing else sets it.
ROOT = os.environ.get("CHECK_ROOT") or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SOURCE_ROOTS = [os.path.join(ROOT, "src", part, "java") for part in ("main", "test")]

# `var` followed by a name is a declaration; `var =` or `var.x` is an identifier called var.
VAR_AS_TYPE = re.compile(r"(?<![\w$.])var\s+[A-Za-z_$]")


def code_only(text):
    """The text with every comment and literal blanked, newlines kept, so `var` in prose is not flagged."""
    out = []
    index = 0
    while index < len(text):
        rest = text[index:]
        if rest.startswith("//"):
            end = text.find("\n", index)
            end = len(text) if end == -1 else end
        elif rest.startswith("/*"):
            end = text.find("*/", index + 2)
            end = len(text) if end == -1 else end + 2
        elif rest.startswith('"""'):
            end = text.find('"""', index + 3)
            end = len(text) if end == -1 else end + 3
        elif rest[0] in "\"'":
            end = index + 1
            while end < len(text) and text[end] not in (rest[0], "\n"):
                end += 2 if text[end] == "\\" else 1
            end += 1
        else:
            out.append(rest[0])
            index += 1
            continue
        out.append(re.sub(r"[^\n]", " ", text[index:end]))
        index = end
    return "".join(out)


def main():
    roots = [root for root in SOURCE_ROOTS if os.path.isdir(root)]
    if not roots:
        print(f"no sources under {os.path.join(ROOT, 'src')} - this rule is checking nothing, which is not passing")
        return 1

    scanned, failures = 0, []
    for root in roots:
        for dirpath, _, filenames in os.walk(root):
            for name in sorted(filenames):
                if not name.endswith(".java"):
                    continue
                path = os.path.join(dirpath, name)
                scanned += 1
                code = code_only(open(path, encoding="utf-8").read())
                for number, line in enumerate(code.split("\n"), 1):
                    if VAR_AS_TYPE.search(line):
                        failures.append((os.path.relpath(path, ROOT), number))

    for path, line in failures:
        print(f"{path}:{line}: var instead of a type")

    if failures:
        print(f"\n{len(failures)} var(s). Write the type out - ADR 0045.")
        return 1

    print(f"ok: {scanned} source(s) write every type out")
    return 0


if __name__ == "__main__":
    sys.exit(main())
