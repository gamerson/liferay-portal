"""Renders a ClientExtension list as a Markdown table for the status report."""

import json
import os
import sys


def condition(item, name):
    for entry in item.get("status", {}).get("conditions", []):
        if entry["type"] == name:
            return entry

    return {}


def main():
    document = json.load(sys.stdin)
    items = sorted(document.get("items", []), key=lambda item: item["metadata"]["name"])

    if not items:
        namespace = os.environ.get("REPORT_NAMESPACE", "")

        print("_No client extensions in `%s`._" % namespace)

        return

    counts = {"Delivered": 0, "Provisioned": 0, "Ready": 0}

    rows = []

    for item in items:
        name = item["metadata"]["name"]
        spec = item.get("spec", {})
        workload = spec.get("workload") or {}
        kind = workload.get("kind", "none")

        delivered = condition(item, "Delivered").get("status", "-")
        provisioned = condition(item, "Provisioned").get("status", "-")
        phase = item.get("status", {}).get("phase", "-")

        counts["Delivered"] += delivered == "True"
        counts["Provisioned"] += provisioned == "True"
        counts["Ready"] += phase == "Ready"

        buckets = len(item.get("status", {}).get("extProvisionConfigMapNames", []) or [])

        note = ""

        if phase != "Ready":
            note = condition(item, "Ready").get("reason", "") or condition(
                item, "Delivered"
            ).get("reason", "")

        rows.append(
            "| `%s` | %s | %d | %s | %s | %s | %s |"
            % (name, kind, buckets, delivered, provisioned, phase, note)
        )

    total = len(items)

    print(
        "**%d client extensions** — Delivered %d/%d, Provisioned %d/%d, Ready %d/%d."
        % (
            total,
            counts["Delivered"],
            total,
            counts["Provisioned"],
            total,
            counts["Ready"],
            total,
        )
    )
    print()
    print("| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |")
    print("|---|---|---|---|---|---|---|")

    for row in rows:
        print(row)


main()