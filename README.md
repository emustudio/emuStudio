# ![emuStudio logo](resources/logo-white.png "emuStudio logo") emuStudio

![emuStudio Build](https://github.com/emustudio/emuStudio/workflows/emuStudio%20Build/badge.svg)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

[emuStudio](https://www.emustudio.net/) is a modular desktop application for emulating historic and teaching-oriented computers and for writing, loading, and debugging programs for them ("compile, load, emulate" workflow). It is built around a plugin model, so compilers, CPUs, memories, and devices can be combined into complete virtual computers.

emuStudio is used both as a learning tool, but also as a base for emulator development, providing well-documented and rich SDK. Emulated computers are called "virtual computers". Often they are full system, feature-rich and cycle-accurate emulators, capable of running original games or software.

emuStudio is very appropriate for use at schools as teaching tool. For example, emuStudio is used at the [Technical University of Košice](http://www.fei.tuke.sk/en) since 2007.

## What is included

- Desktop application with computer schema editor, source code editor and emulation debugger
- CLI entry point for launching configured virtual computers and running automation
- Official compiler, CPU, memory, and device plugins
- Bundled computer configurations and example files shipped with the distribution

### Bundled virtual computers

- [MITS Altair 8800](https://www.emustudio.net/documentation/user/altair8800/)
- [Manchester SSEM](https://www.emustudio.net/documentation/user/ssem/)
- [Random Access Machine (RAM)](https://www.emustudio.net/documentation/user/ram/)
- [Random Access Stored Program (RASP)](https://www.emustudio.net/documentation/user/rasp/)
- [BrainDuck](https://www.emustudio.net/documentation/user/brainduck/), a Brainfuck-oriented teaching machine
- ZX Spectrum 48K (in development, not included in releases yet)

Bundled configuration files live in [`application/src/main/files/config`](application/src/main/files/config).

## Getting started

At first, download a packaged release from <https://www.emustudio.net/download/>.

Prerequisite is to have:
- Java 11 or newer
- Linux, Windows or Mac 

After unpacking the release package, start emuStudio with:

- Linux, Mac: `./emuStudio`
- Windows: `emuStudio.bat`

Useful CLI entry points:

```bash
./emuStudio --help
./emuStudio --computers-list
./emuStudio automation --help
```

## Building and contributing

Build the full project:

```bash
./gradlew build
```

Create a distributable archive:

```bash
./gradlew :application:distZip
./gradlew :application:distTar
```

Distribution archives are written to `application/build/distributions/`.

### Related projects

There are several related projects used by emuStudio that contributors should be familiar with:

- [emuLib](https://github.com/emustudio/emuLib), shared runtime library (defines plugins API, common data structures, and utilities)
- [Edigen](https://github.com/emustudio/edigen), instruction decoder and disassembler generator (used for CPU plugins)
- [Edigen Gradle plugin](https://github.com/emustudio/edigen-gradle-plugin)
- [CPU testing suite](https://github.com/emustudio/cpu-testsuite), framework for writing and running CPU instruction tests
- [emuStudio website](https://github.com/emustudio/emustudio.github.io), source for the project website and user/developer documentation

## Documentation

- User documentation: <https://www.emustudio.net/documentation/user/introduction/>
- Contributing guide: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Developer documentation: <https://emustudio.github.io/documentation/developer/getting_started/contributing>
- Release notes: [`RELEASES.md`](RELEASES.md)
- Plugin-specific notes: README files inside individual plugin directories

## Acknowledgements

emuStudio builds on documentation, research, and emulator work from projects and communities such as:

- [simh](http://simh.trailing-edge.com/)
- [MAME](https://www.mamedev.org/)
- [David Sharp's SSEM site](https://www.davidsharp.com/baby/)
- [Esolang's Brainfuck site](https://esolangs.org/wiki/Brainfuck)
- [DeRamp Altair](https://deramp.com/altair.html)
- [Altair Clone](https://altairclone.com/)
- [Study of techniques for emulation programming](http://www.xsim.com/papers/Bario.2001.emubook.pdf)
- [Intel 8080 instruction table](https://tobiasvl.github.io/optable/intel-8080/classic)
- [ZX-Poly emulator](https://github.com/raydac/zxpoly)
- [Patrik Rak's z80 test suite](https://github.com/raxoft/z80test)
- [ZXSpectrum Next tests](https://github.com/MrKWatkins/ZXSpectrumNextTests/tree/develop)

...and many more! Thanks to all the emulator developers, documenters, and researchers who have shared their work and made projects like emuStudio possible.
