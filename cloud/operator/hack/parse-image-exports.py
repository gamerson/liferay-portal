"""Turns a dump of image manifests into a package to version map."""

import json
import re
import sys

_VERSION_PATTERN = re.compile(r'version="?([0-9]+(?:\.[0-9]+)*)')


def main():
    with open(sys.argv[1], encoding="utf-8", errors="replace") as raw_file:
        raw = raw_file.read()

    exports = {}
    manifests = 0

    for chunk in raw.split("===JAR=== "):
        if "\n" not in chunk:
            continue

        manifest = chunk.split("\n", 1)[1]

        # Unfold the 72 column continuation lines.
        manifest = manifest.replace("\r\n", "\n").replace("\n ", "")

        match = re.search(r"^Export-Package: (.*)$", manifest, re.MULTILINE)

        if not match:
            continue

        manifests += 1

        for clause in split_clauses(match.group(1)):
            package = clause.split(";")[0].strip()

            if not package or " " in package or not package[0].isalpha():
                continue

            version_match = _VERSION_PATTERN.search(clause)

            if not version_match:
                continue

            version = version_match.group(1)
            existing = exports.get(package)

            if existing is None or version_key(version) > version_key(existing):
                exports[package] = version

    with open(sys.argv[2], "w") as exports_file:
        json.dump(exports, exports_file, indent=1, sort_keys=True)

    print(
        "recorded %d exported packages from %d manifests"
        % (len(exports), manifests)
    )


def split_clauses(header):
    clauses = []
    quoted = False
    current = ""

    for character in header:
        if character == '"':
            quoted = not quoted

        if character == "," and not quoted:
            clauses.append(current)
            current = ""
        else:
            current += character

    if current:
        clauses.append(current)

    return clauses


def version_key(value):
    return tuple(int(part) for part in value.split("."))


main()