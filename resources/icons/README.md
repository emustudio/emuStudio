# emuStudio icons

Tango-style icons, **22×22** with a **44×44 `@2x`** variant for HiDPI. `GUI.loadIcon` combines both.
Presentation images (welcome logo, ADM-3A background) keep their own size.

Application and shared plugin icons live in emuLib `/net/emustudio/emulib/runtime/ui/icons/` and are
referenced via `StandardIcons`. A plugin keeps an icon in its own `gui/` package only if no other module uses it.

File names follow constant names: `StandardIcons.ICON_NEW_FILE` is `new-file.png`. Icons with the same artwork
share one file and get alias constants (`ICON_PLAY = ICON_RUN`). Masters in this directory keep upstream Tango names.

`manifest.json` maps each target directory and file name to its master (`"<master> flop"` mirrors it).
Edit a master or the manifest, then regenerate (Python 3 + ImageMagick; emuLib checked out alongside):

```sh
python3 resources/icons/generate.py          # write PNGs
python3 resources/icons/generate.py --check  # verify
```

PNG masters provide the 22px images; matching SVGs render the 44px variants. SVG-only masters are
emuStudio-specific art. Tango artwork ([0.8.90](https://tango.freedesktop.org/releases/tango-icon-theme-0.8.90.tar.gz))
is public domain (`TANGO-COPYING`, `TANGO-AUTHORS`); new SVGs and the generator are GPL-3.0-or-later.
