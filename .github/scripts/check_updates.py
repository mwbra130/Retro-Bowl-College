#!/usr/bin/env python3
import io
import json
import os
import re
import urllib.request
import zipfile
from pathlib import Path
from google_play_scraper import app as gp_app

API_BASE = os.environ.get("GITHUB_API_URL", "https://github.com").rstrip("/")
REPO = os.environ["GITHUB_REPOSITORY"].strip()
TOKEN = os.environ["GITHUB_TOKEN"].strip()
MIN_PATCH_JUMP = 5


def parse_version(v):
    nums = [int(x) for x in re.findall(r"\d+", str(v))]
    while len(nums) < 3:
        nums.append(0)
    return tuple(nums[:3])


def is_meaningful_update(old_ver, new_ver):
    if not new_ver or not re.search(r"\d", str(new_ver)):
        return False
    old_t = parse_version(old_ver)
    new_t = parse_version(new_ver)
    if new_t <= old_t:
        return False
    # Alert on any Major or Minor bump (e.g., 1.2.x -> 1.3.0 or 2.0.0)
    if new_t[0] > old_t[0] or new_t[1] > old_t[1]:
        return True
    # Only alert on Patch bump if it jumped by MIN_PATCH_JUMP or more
    return (new_t[2] - old_t[2]) >= MIN_PATCH_JUMP


def get_latest_version(pkg):
    # 1. Try Google Play API via google-play-scraper
    try:
        info = gp_app(pkg, lang="en", country="us")
        ver = info.get("version")
        if ver and ver != "Varies with device":
            return ver
    except Exception:
        pass

    # 2. Fallback to APKPure if Play Store says "Varies with device" or fails
    try:
        url = f"https://apkpure.net{pkg}"
        req = urllib.request.Request(url, headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
        })
        with urllib.request.urlopen(req, timeout=15) as resp:
            html = resp.read().decode("utf-8", "ignore")
        m = re.search(r'data-dt-version="([^"]+)"', html)
        if m:
            return m.group(1)
    except Exception:
        pass

    return None


def github_api(path, method="GET", body=None):
    url = f"{API_BASE}/repos/{REPO}{path}"
    data = json.dumps(body).encode("utf-8") if body else None
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
            "User-Agent": "morphe-apk-checker",
        },
    )
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def load_patches_json():
    local_file = Path("patches-list.json")
    if local_file.exists():
        return json.loads(local_file.read_text(encoding="utf-8"))

    latest = github_api("/releases/latest")
    for asset in latest.get("assets", []):
        name = asset["name"]
        if name == "patches-list.json":
            req = urllib.request.Request(asset["browser_download_url"], headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=20) as resp:
                return json.loads(resp.read().decode("utf-8"))
        if name.endswith(".mpp"):
            req = urllib.request.Request(asset["browser_download_url"], headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=30) as resp:
                mpp_bytes = resp.read()
            with zipfile.ZipFile(io.BytesIO(mpp_bytes)) as zf:
                if "patches-list.json" in zf.namelist():
                    return json.loads(zf.read("patches-list.json").decode("utf-8"))
    return None


data = load_patches_json()
if not data:
    print("Could not find patches-list.json in repo or latest .mpp release.")
    exit(0)

open_issues = github_api("/issues?state=open&per_page=100")

checked = {}
for patch in data.get("patches", []):
    for cp in patch.get("compatiblePackages") or []:
        pkg = cp["packageName"]
        name = cp.get("name") or pkg
        targets = [t["version"] for t in cp.get("targets", []) if t.get("version")]
        if not targets or pkg in checked:
            continue
        latest_supported = sorted(targets, key=parse_version)[-1]
        checked[pkg] = (name, latest_supported)

for pkg, (name, supported_ver) in checked.items():
    play_ver = get_latest_version(pkg)
    if not play_ver or not is_meaningful_update(supported_ver, play_ver):
        print(f"Skipping {name}: supported={supported_ver}, latest={play_ver}")
        continue

    if any(f"({pkg})" in issue["title"] for issue in open_issues):
        print(f"Open update issue already exists for {name} ({pkg})")
        continue

    issue_title = f"Update {name} ({pkg}) to v{play_ver}"
    print(f"Opening issue: {issue_title}")
    github_api(
        "/issues",
        method="POST",
        body={
            "title": issue_title,
            "labels": ["app-update"],
            "body": (
                f"### Meaningful APK Update Detected\n\n"
                f"- **App:** {name}\n"
                f"- **Package:** `{pkg}`\n"
                f"- **Currently Supported:** `{supported_ver}`\n"
                f"- **Latest Version Found:** `{play_ver}`\n\n"
                f"*Auto-generated by the daily APK version tracker (ignores minor hotfixes).*"
            ),
        },
    )
