# emuStudio Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop

Java Swing desktop application. The user explicitly approved `desktop` because
Impeccable's current platform vocabulary does not cover this application.

## Users

Give equal priority to students and teachers, retrocomputing users, and emulator
and plugin developers.

- Students explore computer architecture and assembly programming by writing,
  compiling, running, and debugging programs.
- Teachers demonstrate machine behavior and use virtual computers for exercises.
- Retrocomputing users explore historic computers and run their software.
- Developers build compiler, CPU, memory, and device plugins, or compose them into
  complete virtual computers using the emuLib SDK.

## Product Purpose

Make historic and teaching-oriented computers accessible for learning,
experimentation, and emulator development. The central workflow is
**compile → load → emulate**. Success means users can prepare a program, run it
on the intended virtual computer, and inspect its behavior.

## Positioning

emuStudio combines a computer schema editor, source editor, and emulation debugger
with a plugin model. A virtual computer is assembled from compiler, CPU, memory,
and device plugins rather than being limited to one fixed emulated machine.

## Operating Context

- Users select or configure a virtual computer, open or write source, compile and
  load it, then run or step through execution and inspect memory and devices.
- The desktop GUI supports interactive work; the CLI supports configured launches
  and headless automation where plugins support it.
- Work uses local source files, TOML configurations, plugin JARs, example programs,
  and device media. Devices may use the host screen, keyboard, filesystem, and audio.
- The application runs on Java on Linux, Windows, and macOS. macOS support is
  official, as confirmed by the user. The application build targets Java 11.

## Capabilities and Constraints

- Preserve the modular monolith architecture and clear internal boundaries.
- Keep shared plugin APIs, runtime services, and reusable UI helpers in emuLib;
  do not leak desktop application internals into plugins or shared libraries.
- Keep the desktop application, bundled plugins, computer configurations, and
  packaging in this repository. Website and user/developer documentation belong
  in the sibling `emustudio.github.io` repository.
- Prefer small changes, explicit design, and ports-and-adapters at UI, filesystem,
  audio, and plugin-loading boundaries. New frameworks and major dependencies
  require discussion.
- Use the term **virtual computer** for a configured emulated computer.
- Emulation fidelity and supported features depend on the chosen plugins; do not
  imply every virtual computer has the same capabilities or timing accuracy.
- No product-specific accessibility standard or additional user needs were
  established during initialization.

## Brand Commitments

Preserve the name **emuStudio**. Existing logo and icon assets are available in
`resources/`; preserve their attribution and licensing requirements when used.

## Evidence on Hand

- `README.md`: product overview, bundled computers, launch/build instructions,
  and related repositories.
- `docs/context.adoc`: users, external systems, and the core workflow. Its
  student-first wording predates the equal-priority audience decision above.
- `AGENTS.md`: architecture, repository responsibilities, and change policies.
- `application/src/main/files/config/`: bundled virtual computer configurations.
- `application/src/main/files/examples/`: runnable example source material.
- `resources/logo.png`, `resources/logo-white.png`, and `resources/icons/`:
  existing identity assets; icon attribution lives alongside them.
- [GitHub issue #433](https://github.com/emustudio/emuStudio/issues/433): release
  QA plan and the user-selected ticket for this initialization. A plan is not
  evidence that its checks passed.

## Product Principles

1. Serve learning, retrocomputing, and emulator development equally.
2. Make the compile, load, and emulate workflow clear and inspectable.
3. Keep virtual computers extensible through explicit plugin contracts.
4. Preserve desktop workflows and repository boundaries through incremental changes.
