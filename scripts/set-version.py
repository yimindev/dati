#!/usr/bin/env python3
"""
Update DatI project version across all modules and configuration files.

Usage:
    ./scripts/set-version.sh <new_version>
    python3 scripts/set-version.py <new_version>
"""

import sys
import re
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent


def get_current_version() -> str:
    root_pom = ROOT_DIR / "pom.xml"
    content = root_pom.read_text(encoding="utf-8")
    match = re.search(
        r"<artifactId>dati-parent</artifactId>\s*<version>([^<]+)</version>",
        content,
    )
    if match:
        return match.group(1)
    raise ValueError("Could not find current version in root pom.xml")


def update_file(path: Path, pattern: str, replacement: str) -> bool:
    if not path.exists():
        print(f"  [SKIP] File not found: {path.relative_to(ROOT_DIR)}")
        return False
    content = path.read_text(encoding="utf-8")
    new_content, count = re.subn(pattern, replacement, content)
    if count == 0:
        print(f"  [WARN] Pattern not matched in: {path.relative_to(ROOT_DIR)}")
        return False
    path.write_text(new_content, encoding="utf-8")
    print(
        f"  [OK] Updated ({count} match{'es' if count > 1 else ''}): {path.relative_to(ROOT_DIR)}"
    )
    return True


def main():
    if len(sys.argv) != 2 or sys.argv[1] in ("-h", "--help"):
        curr = get_current_version()
        print(f"Current version: {curr}")
        print("\nUsage:")
        print("    ./scripts/set-version.sh <new_version>")
        print("    python3 scripts/set-version.py <new_version>")
        print("\nExample:")
        print("    ./scripts/set-version.sh 0.5.0")
        sys.exit(0 if len(sys.argv) == 2 and sys.argv[1] in ("-h", "--help") else 1)

    new_version = sys.argv[1].strip()
    curr_version = get_current_version()

    if new_version == curr_version:
        print(f"Version is already {curr_version}. No changes made.")
        sys.exit(0)

    print(f"Updating DatI version: {curr_version} -> {new_version}\n")

    # 1. Root pom.xml
    update_file(
        ROOT_DIR / "pom.xml",
        r"(<artifactId>dati-parent</artifactId>\s*<version>)[^<]+(</version>)",
        rf"\g<1>{new_version}\g<2>",
    )

    # 2. Submodule pom.xml files
    pom_files = [
        ROOT_DIR / "backend" / "pom.xml",
        ROOT_DIR / "backend" / "core" / "pom.xml",
        ROOT_DIR / "backend" / "app" / "pom.xml",
        ROOT_DIR / "backend" / "ext" / "auth-apikey" / "pom.xml",
        ROOT_DIR / "backend" / "ext" / "auth-local" / "pom.xml",
    ]
    for pom in pom_files:
        update_file(
            pom,
            r"(<parent>\s*<groupId>com\.dati</groupId>\s*<artifactId>dati-parent</artifactId>\s*<version>)[^<]+(</version>)",
            rf"\g<1>{new_version}\g<2>",
        )

    # 3. SpringDocConfig.java
    update_file(
        ROOT_DIR
        / "backend"
        / "core"
        / "src"
        / "main"
        / "java"
        / "com"
        / "dati"
        / "config"
        / "SpringDocConfig.java",
        r'(\.version\(")[^"]+("\))',
        rf"\g<1>{new_version}\g<2>",
    )

    # 4. Frontend package.json
    update_file(
        ROOT_DIR / "frontend" / "package.json",
        r'("version":\s*")[^"]+(")',
        rf"\g<1>{new_version}\g<2>",
    )

    # 5. OpenAPI JSON files
    openapi_files = [
        ROOT_DIR / "docs" / "api" / "openapi.json",
        ROOT_DIR / "skills" / "dati-ops" / "openapi.json",
    ]
    for oapi in openapi_files:
        if oapi.exists():
            update_file(
                oapi,
                r'("info"\s*:\s*\{[^}]*?"version"\s*:\s*")[^"]+(")',
                rf"\g<1>{new_version}\g<2>",
            )

    print(f"\nDone! Version successfully updated to {new_version}.")


if __name__ == "__main__":
    main()
