#!/usr/bin/env python3
"""Parse Qodana SARIF report and display results in a readable format.

Usage:
    python3 tools/qodana-report.py [OPTIONS] [SARIF_FILE]

Arguments:
    SARIF_FILE  Path to qodana.sarif.json (default: target/qodana-results/qodana.sarif.json)

Options:
    -s, --severity LEVEL  Filter by minimum severity: error, warning, note (default: all)
    -g, --group           Group results by rule
    -q, --quiet           Show only summary counts
    -h, --help            Show this help message
"""

import json
import sys
from pathlib import Path

DEFAULT_SARIF = "target/qodana-results/qodana.sarif.json"

SEVERITY_ORDER = {"error": 0, "warning": 1, "note": 2}

# Shorten common path prefixes for readability
PATH_SHORTCUTS = [
    ("proto-wrapper-core/src/main/java/io/alnovis/protowrapper/", "core:"),
    ("proto-wrapper-core/src/test/java/io/alnovis/protowrapper/", "core-test:"),
    ("proto-wrapper-maven-plugin/src/main/java/io/alnovis/protowrapper/", "maven:"),
    ("proto-wrapper-gradle-plugin/src/main/kotlin/io/alnovis/protowrapper/", "gradle:"),
    ("proto-wrapper-spring-boot-starter/", "spring:"),
    ("examples/", "examples:"),
]


def shorten_path(uri: str) -> str:
    for prefix, shortcut in PATH_SHORTCUTS:
        if uri.startswith(prefix):
            return shortcut + uri[len(prefix):]
    return uri


def parse_results(sarif_path: str):
    with open(sarif_path) as f:
        data = json.load(f)

    results = data["runs"][0]["results"]
    parsed = []

    for r in results:
        level = r.get("level", "note")
        rule = r.get("ruleId", "unknown")
        msg = r.get("message", {}).get("text", "")

        loc_str = ""
        locs = r.get("locations", [])
        if locs:
            phys = locs[0].get("physicalLocation", {})
            uri = phys.get("artifactLocation", {}).get("uri", "")
            line = phys.get("region", {}).get("startLine", "?")
            loc_str = f"{shorten_path(uri)}:{line}"

        parsed.append({
            "level": level,
            "rule": rule,
            "message": msg,
            "location": loc_str,
        })

    return sorted(parsed, key=lambda x: SEVERITY_ORDER.get(x["level"], 99))


def print_results(results, min_severity=None, group=False, quiet=False):
    if min_severity:
        threshold = SEVERITY_ORDER.get(min_severity, 99)
        results = [r for r in results if SEVERITY_ORDER.get(r["level"], 99) <= threshold]

    # Summary
    counts = {}
    for r in results:
        counts[r["level"]] = counts.get(r["level"], 0) + 1

    total = len(results)
    summary_parts = []
    for level in ("error", "warning", "note"):
        if level in counts:
            summary_parts.append(f"{counts[level]} {level}s")

    print(f"Total: {total} problems ({', '.join(summary_parts)})")
    print()

    if quiet:
        return

    if group:
        # Group by rule
        by_rule = {}
        for r in results:
            by_rule.setdefault(r["rule"], []).append(r)

        for rule, items in sorted(by_rule.items(), key=lambda x: SEVERITY_ORDER.get(x[1][0]["level"], 99)):
            level = items[0]["level"]
            print(f"[{level:7}] {rule} ({len(items)}x)")
            for item in items:
                print(f"           {item['location']}")
            print()
    else:
        # Flat list
        for r in results:
            level = r["level"]
            loc = r["location"]
            msg = r["message"][:100]
            print(f"{level:7} | {loc:70} | {msg}")


def main():
    args = sys.argv[1:]
    sarif_path = DEFAULT_SARIF
    min_severity = None
    group = False
    quiet = False

    i = 0
    while i < len(args):
        arg = args[i]
        if arg in ("-h", "--help"):
            print(__doc__)
            sys.exit(0)
        elif arg in ("-s", "--severity"):
            i += 1
            min_severity = args[i]
        elif arg in ("-g", "--group"):
            group = True
        elif arg in ("-q", "--quiet"):
            quiet = True
        elif not arg.startswith("-"):
            sarif_path = arg
        else:
            print(f"Unknown option: {arg}", file=sys.stderr)
            sys.exit(1)
        i += 1

    if not Path(sarif_path).exists():
        print(f"SARIF file not found: {sarif_path}", file=sys.stderr)
        print(f"Run 'qodana scan --results-dir=target/qodana-results' first.", file=sys.stderr)
        sys.exit(1)

    results = parse_results(sarif_path)
    print_results(results, min_severity, group, quiet)


if __name__ == "__main__":
    main()
