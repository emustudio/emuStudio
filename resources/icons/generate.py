#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Peter Jakubčo
# SPDX-License-Identifier: GPL-3.0-or-later
"""Regenerate bundled 22px icons from the manifest; --check verifies resources."""

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    sources = Path(__file__).resolve().parent
    root = sources.parent.parent
    manifest = json.loads((sources / "manifest.json").read_text())
    errors = []
    with tempfile.TemporaryDirectory() as directory:
        cache = {}
        for entry in manifest:
            key = (entry["source"], entry.get("flip", False))
            if key not in cache:
                source = sources / entry["source"]
                output = Path(directory) / f"{len(cache)}.png"
                if source.suffix == ".png" and not key[1]:
                    shutil.copyfile(source, output)
                else:
                    command = ["magick", "-background", "none", str(source)]
                    if key[1]:
                        command.append("-flop")
                    command += ["-resize", "22x22!", "-strip", "PNG32:" + str(output)]
                    subprocess.run(command, check=True)
                cache[key] = output.read_bytes()
            target = root / entry["target"]
            if args.check:
                if not target.exists() or target.read_bytes() != cache[key]:
                    errors.append(entry["target"])
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(cache[key])

    # Include resources bypassing the application loader (direct plugin ImageIcons).
    expected = {entry["target"] for entry in manifest}
    backgrounds = {
        "application/src/main/resources/net/emustudio/application/gui/dialogs/logo.png",
        "plugins/device/adm3A-terminal/src/main/resources/net/emustudio/plugins/device/adm3a/gui/display.png",
    }
    for module in [root / "application", *(root / "plugins").glob("*/*")]:
        for image in (module / "src/main/resources").rglob("*"):
            if image.suffix.lower() in {".png", ".gif", ".jpg", ".jpeg"}:
                relative = str(image.relative_to(root))
                if relative not in expected and relative not in backgrounds:
                    errors.append("Unmapped image: " + relative)
    for entry in manifest:
        target = root / entry["target"]
        if target.exists():
            size = subprocess.check_output(
                ["magick", "identify", "-format", "%wx%h", str(target)], text=True)
            if size != "22x22":
                errors.append(f"{entry['target']}: {size}")
    if errors:
        parser.exit(1, "Icon resources need attention:\n" + "\n".join(errors) + "\n")
    print(f"{'Verified' if args.check else 'Generated'} {len(manifest)} icons at 22x22")


if __name__ == "__main__":
    main()
