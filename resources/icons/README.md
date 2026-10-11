# emuStudio icons

UI icons use Tango's palette, shaded fills, dark outlines and light inner highlights.
Every bundled action, menu, status and window icon is **22×22 logical pixels**, with
a **44×44 `@2x` PNG** for HiDPI displays. The welcome logo and ADM-3A terminal
background retain their presentation dimensions.

`manifest.json` maps each application/plugin resource to a shared master here.
Resources remain inside their owning modules; plugins need no application classes
or new runtime libraries to load them.

All source images live directly in `resources/icons/`:

- PNG masters preserve Tango's hand-tuned 22px details and pixel alignment.
  Matching SVGs supply the 44px variants, rendered directly from vector artwork.
- SVG masters cover emulator-specific symbols: CPU, memory, motherboard,
  breakpoints, timed execution, terminal clear, ASCII and device indicators.
  The memory-view and breakpoint-toggle masters preserve the original toolbar
  artwork.

Use descriptive lowercase names with hyphens, such as `memory-view.png`,
`breakpoint-toggle.png` and `indicator-blue.png`. Sizes belong in the `@2x`
suffix rather than in the base name. Keep upstream artwork names unchanged.

Generate bundled PNGs with Python 3 and ImageMagick (asset tools only):

```sh
python3 resources/icons/generate.py
python3 resources/icons/generate.py --check
```

Edit a master, then regenerate both sizes. The shared `GUI.loadIcon` loader
automatically combines each base PNG and its `@2x` variant, retaining the 22px
layout size. Repeated actions such as open, save, playback and volume share the
same artwork across the application and devices. Static GIF icons were replaced
by PNGs to retain smooth transparent edges.

Tango artwork comes from [Tango Icon Theme 0.8.90](https://tango.freedesktop.org/releases/tango-icon-theme-0.8.90.tar.gz).
It is public domain; see `TANGO-COPYING` and `TANGO-AUTHORS`. New emuStudio SVGs
and the generator use GPL-3.0-or-later, as marked in their source headers.
