# emuStudio icons

UI icons use Tango's palette, shaded fills, dark outlines and light inner highlights.
Every bundled action, menu, status and window icon is **22×22 pixels**. The welcome
logo and ADM-3A terminal background retain their presentation dimensions.

`manifest.json` maps each application/plugin resource to a shared master here.
Resources remain inside their owning modules; plugins need no application classes
or new runtime libraries to load them.

- `tango/*.png`: upstream's hand-tuned 22px masters, used directly to preserve small
  details and pixel alignment. Matching `tango/*.svg` files are the upstream
  scalable artwork, useful when drawing adaptations or exporting larger sizes.
- `emustudio/*.svg`: editable 22px masters for emulator-specific symbols: CPU,
  memory, motherboard, breakpoints, timed execution, terminal clear, ASCII and
  device indicators. These use the same Tango palette and drawing conventions.

Generate bundled PNGs with Python 3 and ImageMagick (asset tools only):

```sh
python3 resources/icons/generate.py
python3 resources/icons/generate.py --check
```

Edit a master, then regenerate. Repeated actions such as open, save, playback and
volume share the same artwork across the application and devices. Static GIF
icons were replaced by PNGs to retain smooth transparent edges.

Tango artwork comes from [Tango Icon Theme 0.8.90](https://tango.freedesktop.org/releases/tango-icon-theme-0.8.90.tar.gz).
It is public domain; see `TANGO-COPYING` and `TANGO-AUTHORS`. New emuStudio SVGs
and the generator use GPL-3.0-or-later, as marked in their source headers.
