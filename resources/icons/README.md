# emuStudio icons

SVG sources grouped by function. Each source has generated **22×22** `.png` and
**44×44** `@2x.png` siblings. `GUI.loadIcon` combines both sizes.

Application and shared plugin icons live in emuLib `/net/emustudio/emulib/runtime/ui/icons/` and are
referenced via `StandardIcons`. A plugin keeps an icon in its own `gui/` package only if no other module uses it.

Categories: `text`, `emulation`, `inspection`, `pagination`, `virtual-computer`,
`memory`, `audio`, `status`, `application`, and `plugins/<plugin-name>`.
Names describe emuStudio functions: `emulation/run-emulation.svg`,
`text/new-file.svg`, `plugins/byte-mem/go-to-address.svg`.

`manifest.json` maps packaged resource paths to categorized SVG sources
(`"<source> flop"` mirrors it). Runtime resource names are mapped independently
of catalog names. Edit SVGs or the manifest, then regenerate
(Python 3 + ImageMagick; emuLib checked out alongside):

```sh
python3 resources/icons/generate.py          # write PNGs
python3 resources/icons/generate.py --check  # verify
```

Both sizes render directly from SVGs. Inspection uses one icon set.
Presentation images (welcome logo, ADM-3A background) keep their own size.

Tango artwork is public domain (`TANGO-COPYING`, `TANGO-AUTHORS`);
emuStudio-specific SVGs and the generator are GPL-3.0-or-later.
