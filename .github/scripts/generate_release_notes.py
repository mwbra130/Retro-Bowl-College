#!/usr/bin/env python3
"""Generate the GitHub release body for a Morphe bundle release.

Compares the previous release's patches-list.json with the new one and
renders:

    Updates:
    - **App Name**: Patch name - patch description

    Bug Fixes:
    - **App Name**: what the fix commit says

Rules:
- A patch that is new in the new list (or whose description changed) is an
  Update. Its line is copied from the patch's own name + description in
  patches-list.json, grouped under its app.
- A bug fix is an app whose folder has fix(<folder>): commits since the
  previous release, excluding the promote workflow's generic
  "update patches" sync subjects (the real change in a promote shows up in
  the patch-list diff instead). The line uses the fix commit's subject.
- Empty sections are omitted. If nothing changed, the body says so.

Usage:
  generate_release_notes.py --new-list patches-list.json \
      --old-list /tmp/old-list.json --subjects /tmp/subjects.txt \
      --patches-dir patches/src/main/kotlin/app/zdrgon/patches
"""

import argparse
import json
import re
import sys
from pathlib import Path


def load_patches(path):
    """Return {(package, patch_name): (app_name, description)}."""
    if not path or not Path(path).exists():
        return {}
    data = json.loads(Path(path).read_text())
    out = {}
    for patch in data.get("patches", []):
        pkgs = patch.get("compatiblePackages") or []
        if pkgs:
            for cp in pkgs:
                out[(cp["packageName"], patch["name"])] = (
                    cp.get("name") or cp["packageName"],
                    patch.get("description") or "",
                )
        else:
            out[("", patch["name"])] = ("Universal", patch.get("description") or "")
    return out


def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--new-list", required=True)
    ap.add_argument("--old-list")
    ap.add_argument("--subjects")
    ap.add_argument("--patches-dir")
    args = ap.parse_args()

    new = load_patches(args.new_list)
    old = load_patches(args.old_list)

    # ---- Updates: new patches + description changes -----------------------
    updates = {}  # app -> [(patch, description)]
    for key, (app, desc) in new.items():
        if key not in old or old[key][1] != desc:
            updates.setdefault(app, []).append((key[1], desc))

    # ---- Bug fixes: fix(<folder>): subjects ------------------------------
    fixes = {}  # display app -> [subject text]
    if args.subjects and args.patches_dir:
        folders = {p.name for p in Path(args.patches_dir).iterdir() if p.is_dir()}
        # folder -> display name, resolved against the list's app names
        display = {}
        app_names = {app for app, _ in new.values()}
        for folder in folders:
            match = None
            for name in app_names:
                n = norm(name)
                if n == folder or folder in n or n in folder:
                    match = name
                    break
            display[folder] = match or folder[:1].upper() + folder[1:]
        seen = set()
        for line in Path(args.subjects).read_text().splitlines():
            m = re.match(r"^fix\(([a-z0-9_-]+)\):\s*(.+)$", line.strip())
            if not m:
                continue
            folder, text = m.group(1), m.group(2).strip()
            if folder not in folders:
                continue  # workflow/checker/docs scopes are not apps
            if text.lower() == "update patches":
                continue  # promote sync placeholder; real change is in the diff
            app = display[folder]
            if (app, text) not in seen:
                seen.add((app, text))
                fixes.setdefault(app, []).append(text)

    # ---- Render ------------------------------------------------------------
    lines = []
    if updates:
        lines.append("Updates:")
        for app in sorted(updates):
            for patch_name, desc in sorted(updates[app]):
                lines.append(f"- **{app}**: {patch_name} — {desc}")
        lines.append("")
    if fixes:
        lines.append("Bug Fixes:")
        for app in sorted(fixes):
            for text in fixes[app]:
                lines.append(f"- **{app}**: {text}")
        lines.append("")
    if not lines:
        lines = ["No patch changes in this release."]
    sys.stdout.write("\n".join(lines).rstrip() + "\n")


if __name__ == "__main__":
    main()
