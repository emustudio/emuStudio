---
name: emuStudio
description: An instrument console with the usability of a computer lab.
colors:
  primary: '#478ac9'
  primary-dark: '#375a81'
  primary-hover: '#3b82c6'
  primary-pressed: '#3270ab'
  button-hover: '#f7f7f7'
  button-pressed: '#e6e6e6'
  tab-active: '#3c83c5'
  tab-active-dark: '#4c87c8'
  selection: '#2675bf'
  selection-dark: '#4b6eaf'
  focus: '#98c3eb'
  focus-dark: '#3c628c'
  panel: '#f2f2f2'
  panel-dark: '#3c3f41'
  field: '#ffffff'
  field-dark: '#46494b'
  text: '#000000'
  text-dark: '#dddddd'
  border: '#c2c2c2'
  border-dark: '#616365'
  button-dark: '#4e5052'
  muted: '#808080'
  muted-dark: '#a6a6a6'
  debug-row: '#f1f5fa'
  current-instruction: '#ff0000'
  cpu-running: '#009933'
  schema-compiler: '#eeefff'
  schema-cpu: '#ffeeee'
  schema-memory: '#eeeeee'
  schema-device: '#e0e0e0'
  schema-selected: '#0000ff'
typography:
  body:
    fontFamily: Dialog, sans-serif
    fontSize: 12px
    fontWeight: 400
  title:
    fontFamily: Dialog, sans-serif
    fontSize: 14px
    fontWeight: 700
  label:
    fontFamily: SansSerif, sans-serif
    fontSize: 12px
    fontWeight: 700
  data:
    fontFamily: Monospaced, monospace
    fontSize: 12px
    fontWeight: 400
  code:
    fontFamily: JetBrains Mono NL, monospace
    fontSize: 14px
    fontWeight: 400
rounded:
  control: 4px
spacing:
  button-margin-x: 14px
  button-margin-y: 2px
  field-margin-x: 6px
  field-margin-y: 2px
  tab-inset-x: 12px
  tab-inset-y: 4px
  list-inset: 2px
components:
  button:
    backgroundColor: '{colors.field}'
    textColor: '{colors.text}'
    rounded: '{rounded.control}'
    typography: '{typography.body}'
  button-primary:
    backgroundColor: '{colors.primary}'
    textColor: '{colors.field}'
    rounded: '{rounded.control}'
  button-primary-dark:
    backgroundColor: '{colors.primary-dark}'
    textColor: '{colors.text-dark}'
    rounded: '{rounded.control}'
  button-primary-light:
    backgroundColor: '{colors.field}'
    textColor: '{colors.text}'
    rounded: '{rounded.control}'
  input:
    backgroundColor: '{colors.field}'
    textColor: '{colors.text}'
    rounded: '{rounded.control}'
    typography: '{typography.data}'
  input-dark:
    backgroundColor: '{colors.field-dark}'
    textColor: '{colors.text-dark}'
    rounded: '{rounded.control}'
    typography: '{typography.data}'
---

# Design System: emuStudio

## Overview

**Creative North Star: "Instrument Console"**

An instrument console for examining computers, with the usability of a computer lab. The visual character is technical, focused, and information-dense; the interaction character is approachable, instructional, and orderly. Familiar desktop controls, explicit labels, and visible state make complex work navigable.

This records the existing Swing interface, not a replacement visual direction. The application shell uses FlatLaf, compact toolbars, tabs, split panes, and titled sections. Emulator displays and hardware-specific device windows retain their own visual languages. No additional visual anti-reference was established.

**Key Characteristics:**

- Compact technical workspaces with familiar desktop controls.
- Instrument blue and graphite neutrals, with semantic status colors.
- Mostly flat surfaces organized by borders and tonal layers.
- Precise, familiar, approachable controls with clear labels and visible state.

This is an incumbent design capture under ticket [#433](https://github.com/emustudio/emuStudio/issues/433).
The user confirmed the metaphor, computer-lab UX emphasis, color character, depth,
and component language. Product and platform facts live in [PRODUCT.md](PRODUCT.md).

Evidence: current `EmuStudioGui`, `REditor`, `StudioFrame`, schema elements, and
`TextCellRenderer`; shared constants in the sibling emuLib checkout; and
`resources/icons/README.md`. Current-source GUI factories were rendered offscreen
with FlatLaf 3.7.1 in all three themes at 1× scale. Existing source-editor and
88-HDSK settings screenshots in the sibling website repository supplied full-screen
context; they are visual references, not proof of every current behavior.
This capture does not establish Windows/macOS rendering parity or accessibility conformance.

The [DESIGN.md format](https://raw.githubusercontent.com/google-labs-code/design.md/main/docs/spec.md)
provides the portable token structure. In this Swing project, `px` denotes a
1× Java user-space measurement, not a fixed physical size under HiDPI scaling.
Logical Java font names resolve per OS. Frontmatter records observed values;
Java sources and active look-and-feel defaults govern implementation.

## Colors

Instrument blue supplies interaction cues; graphite neutrals carry the workspace;
semantic colors identify machine state. Frontmatter holds exact values.

### Primary

| Character | Tokens | Existing use |
| --- | --- | --- |
| Instrument Blue | `primary`, `primary-dark` | Filled default buttons in IntelliJ and Dark respectively. |
| Navigation Blue | `tab-active`, `tab-active-dark` | Active-tab underline. |
| Selection Blue | `selection`, `selection-dark` | Standard table selection. |
| Focus Blue | `focus`, `focus-dark` | Look-and-feel focus border roles. |

Light uses a white default-button background rather than IntelliJ's blue fill.
The default theme is IntelliJ. Do not collapse the three themes into one palette.

### Neutral

| Character | Tokens | Existing use |
| --- | --- | --- |
| Workbench Gray / Graphite | `panel`, `panel-dark` | Panel and tab-content surfaces. |
| Paper / Graphite Field | `field`, `field-dark` | Editable fields; light ordinary buttons use Paper too. |
| Ink / Chalk | `text`, `text-dark` | Main shell foreground. |
| Fine Gray / Graphite Edge | `border`, `border-dark` | Standard component borders. |
| Graphite Button | `button-dark` | Ordinary Dark-theme button surface. |
| Muted Gray | `muted`, `muted-dark` | Disabled button text and inactive field foreground. |

### Semantic and canvas colors

- `current-instruction`: the debugger's current-instruction row, with white text.
  Red is therefore not exclusively an error signal in this product.
- `cpu-running`: the shared CPU run-state color.
- `debug-row`: the pale alternate row used by the custom debugger renderer;
  other rows are white with black text. This renderer has its own palette.
- `schema-compiler`, `schema-cpu`, `schema-memory`, `schema-device`: pastel node
  fills, each blended from white by the existing vertical gradient.
- `schema-selected`: selection in the schema, distinct from shell Selection Blue.
- Source syntax has fixed comment, literal, label, register, preprocessor, and
  error colors in application `Constants`; preserve those assignments rather than
  substituting shell accent colors. `REditor.setupSyntaxTheme` applies them.

**The Theme Role Rule.** Use active UIManager roles for new shell controls; a sampled theme color is not a replacement for runtime theme resolution.

The sidecar's tonal ramps are generated preview aids, not additional implemented
palette steps. Theme-specific primitives remain in the frontmatter only.

## Typography

| Role | Source and use |
| --- | --- |
| Body | `FONT_COMMON`: logical Dialog, regular; labels, menus, buttons, and tables. |
| Title | `labelTitle`: Dialog bold, two user-space units above its base label size. |
| Label | `FONT_TITLE_BORDER`: SansSerif bold; titled borders, tab labels, table headers. |
| Data | `FONT_MONOSPACED`: logical Monospaced; text fields, text areas, lists, debugger data. |
| Code | `FONT_CODE`: bundled JetBrains Mono NL; source editor, with user-controlled zoom. |

The hierarchy is compact and functional. There is no application-wide display or
hero type scale. Source zoom and font fallback must remain usable. Retain the
bundled font's OFL notice. Schema nodes locally use Dialog bold for names and
Monospaced italic for plugin types; these are diagram annotations, not shell headings.

**The Technical Type Rule.** Keep source, addresses, and machine data monospaced; use the logical dialog font for surrounding controls.

## Layout

The main window has a menu bar and task tabs for the source editor and emulator.
Source-file tabs, compact icon toolbars, and a resizable output split structure
editing. The emulator combines debugger, peripheral-device list, and CPU status
areas in split panes. Settings use label/field rows, titled groups, and explicit
actions; native window resizing and scrolling provide adaptation.

Use MigLayout's existing `insets dialog`, grow/fill constraints, and size groups.
Shell roots often use zero insets while dialogs supply their own margins. There
is no global web grid or mobile breakpoint contract. Platform-derived dialog gaps
are not a newly invented fixed spacing scale.

Frontmatter spacing entries record look-and-feel margins and tab/list insets,
not the total painted padding or touch target. Borders and focus geometry add
space. The observed tab height is 32 units; `styleTable` uses
`max(existingRowHeight, 22) + 4`, giving 26 units in the captured theme samples.
Do not force that sample height onto tables with a larger existing row height.

## Elevation & Depth

Mostly flat: tonal layers, understated borders, titled sections, tab selection,
and split dividers organize content. Let FlatLaf and the OS render popup/window
depth rather than inventing an application shadow scale. The white-to-pastel
schema gradients and raised node outlines are existing local exceptions. The
logo's fading border is also local, not a reusable card shadow.

**The Quiet Surface Rule.** Organize ordinary workspaces with layout, titled borders, tabs, and tone; keep the existing schema gradients local to the schema.

No application-owned shadow, motion-duration, or breakpoint scale was found;
none is fabricated in the sidecar.

## Shapes

Ordinary controls are compact rectangles with gently curved corners. The Java
customization sets `Button.arc`, `Component.arc`, and `TextComponent.arc` to 8.
These are arc diameters; the frontmatter radius is their CSS equivalent, 4 at 1×.
See [FlatLaf's corner documentation](https://www.formdev.com/flatlaf/customizing/).

Titled sections and tab strips use restrained borders rather than rounded cards.
Schema nodes retain square, raised outlines. Do not apply control rounding to
emulated displays, schema geometry, or other hardware-specific canvases.

## Components

### Buttons and toolbars

Precise, familiar, approachable. Use ordinary Swing buttons for named actions.
`buttonMakePrimary` makes a button default-capable and bold; the containing root
pane must choose its default button to obtain default-button styling. IntelliJ,
Light, and Dark supply different default-button appearances.

Hover, pressed, focus, and disabled treatments come from the active look and feel.
The probe found an IntelliJ focus width of 2 versus 0 for Light/Dark; a zero outer
focus width does not mean all focus indications disappear.

Toolbars are non-floating, rollover-enabled, and borderless. Toolbar buttons hide
action text, use tooltips and the FlatLaf toolbar-button type, and are deliberately
non-focusable in the existing implementation. Keep associated menu actions usable.

Icons use the existing Tango-style pipeline: 22×22 with 44×44 `@2x` variants.
Shared application/plugin icons belong to emuLib and are referenced through
`StandardIcons`; `resources/icons/README.md` explains source and attribution.

### Inputs and titled sections

Text fields use the logical monospaced role and the active field/background and
border roles. Labels explain the requested value; Browse and related actions sit
beside file/media fields. Use `gui.section` for a titled group, not a decorative
card. Theme-dependent focus, disabled, and error rendering stays with Swing/FlatLaf
unless a specific existing component has its own validation behavior.

### Tabs and split panes

Tabbed navigation uses bold labels and a blue active underline. Tab content and
panels share a background; selected tabs are not floating cards. Source-file tabs
sit inside the editing workspace. Split panes are continuous-layout,
one-touch-expandable, and borderless through the shared GUI factory.

### Tables and machine state

Standard tables use the theme, compact bold headers, and `styleTable`'s gridless,
zero-intercell-spacing treatment. The debugger's custom text renderer instead
uses monospaced text, fixed alternating rows, and a red current-instruction row.
Selection and machine-state highlighting are distinct concepts; do not assume
all custom renderers inherit standard selection or Dark-theme colors.

### Source editor and schema

The source editor uses syntax highlighting, line numbers, folding and marker
gutters, file tabs, and adjustable text size. Its fixed syntax colors are not a
complete Dark-theme palette contract. Preserve existing source navigation and zoom.

The schema is a white technical canvas with a grid, square pastel plugin nodes,
plugin name/type annotations, connections, and blue selection. It intentionally
has separate colors and geometry from ordinary controls.

The companion `.impeccable/design.json` contains portable HTML/CSS illustrations
of these Swing patterns for Impeccable's panel. They are 1× visual translations,
not executable Swing components, pixel-parity guarantees, or native behavior tests.

## Do's and Don'ts

### Do

- Do use the shared GUI factories and active look-and-feel roles for new application controls.
- Do check IntelliJ, Light, and Dark themes, including focus, selected, and disabled states.
- Do retain readable labels, tooltips, source zoom, and clear machine-state indicators.
- Do preserve monospaced source and machine data and use the existing icon pipeline.
- Do distinguish theme-driven shell styling from fixed editor, debugger, schema, and emulated-display palettes.

### Don't

- Don't turn a task workspace into a marketing layout with oversized display typography or decorative cards.
- Don't interpret FlatLaf arc diameter as a CSS radius or treat Swing margins as total component insets.
- Don't assume Light has the same filled primary button as IntelliJ, or that Dark recolors every custom canvas.
- Don't apply generated preview tonal ramps to the application as if they were implemented tokens.
