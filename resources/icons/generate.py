#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Peter Jakubčo
# SPDX-License-Identifier: GPL-3.0-or-later
"""Regenerate bundled 22px and 44px icons; --check verifies resources."""

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
    manifest = [{"target": f"{directory}/{name}", "source": source.split()[0], "flip": source.endswith(" flop")}
                for directory, icons in json.loads((sources / "manifest.json").read_text()).items()
                for name, source in icons.items()]
    errors = []
    with tempfile.TemporaryDirectory() as directory:
        cache = {}
        for entry in manifest:
            for size in (22, 44):
                key = (entry["source"], entry.get("flip", False), size)
                if key not in cache:
                    source = sources / entry["source"]
                    output = Path(directory) / f"{len(cache)}.png"
                    if size == 22 and source.suffix == ".png" and not key[1]:
                        shutil.copyfile(source, output)
                    else:
                        command = ["magick", "-background", "none"]
                        if size == 44:
                            source = source.with_suffix(".svg")
                            command += ["-density", "192"]
                        command.append(str(source))
                        if key[1]:
                            command.append("-flop")
                        command += ["-resize", f"{size}x{size}!", "-strip", "PNG32:" + str(output)]
                        subprocess.run(command, check=True)
                    cache[key] = output.read_bytes()
                target = root / entry["target"]
                if size == 44:
                    target = target.with_name(target.stem + "@2x" + target.suffix)
                if args.check:
                    if not target.exists() or target.read_bytes() != cache[key]:
                        errors.append(str(target.relative_to(root)))
                else:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(cache[key])

    # Include resources bypassing the application loader (direct plugin ImageIcons).
    expected = {entry["target"] for entry in manifest}
    expected.update(str(Path(target).with_name(Path(target).stem + "@2x.png"))
                    for target in list(expected))
    backgrounds = {
        "plugins/device/adm3A-terminal/src/main/resources/net/emustudio/plugins/device/adm3a/gui/display.png",
    }
    for module in [root / "application", *(root / "plugins").glob("*/*")]:
        for image in (module / "src/main/resources").rglob("*"):
            if image.suffix.lower() in {".png", ".gif", ".jpg", ".jpeg"}:
                relative = str(image.relative_to(root))
                if relative not in expected and relative not in backgrounds:
                    errors.append("Unmapped image: " + relative)
    for relative in sorted(expected):
        target = root / relative
        if target.exists():
            size = subprocess.check_output(
                ["magick", "identify", "-format", "%wx%h", str(target)], text=True)
            expected_size = "44x44" if target.stem.endswith("@2x") else "22x22"
            if size != expected_size:
                errors.append(f"{relative}: {size}")
    if errors:
        parser.exit(1, "Icon resources need attention:\n" + "\n".join(errors) + "\n")
    print(f"{'Verified' if args.check else 'Generated'} {len(manifest)} icon pairs at 22x22 and 44x44")


if __name__ == "__main__":
    main()
