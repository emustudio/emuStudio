# emuStudio logo assets

| Variant | Files | Purpose |
| --- | --- | --- |
| `emustudio` | SVG, PNG (296×288) | Full logo with gold computer detail |
| `symbol` | SVG, PNG (30×30) | Single gold computer |
| `monochrome` | PNG (74×72) | Black logo with transparent background |
| `on-white` | PNG (99×96) | Logo on white background, used in the project README |

SVG and PNG counterparts share a basename. Keep original presentation sizes;
toolbar icons belong in `../icons/`.

`emustudio.mp` is the MetaPost source for the `emustudio`, `symbol`, and
`monochrome` variants. Run `mpost emustudio.mp` from this directory to export
their PNGs. The SVG exports and `on-white.png` are preserved artwork.
