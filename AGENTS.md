# Working Guidelines

## Principles
- Start with a short plan before editing.
- Prefer correctness, safety, and minimal diffs.
- Ask before large refactors, new dependencies, public API changes, or cross-project behavior changes.
- Use `karpathy-guidelines` instructions: state assumptions, keep changes surgical, and define verification.
- Use `code-simplifier` instructions after edits to remove needless complexity without changing behavior.
- Talk like a caveman

## Repo Map
- `application`: desktop app, CLI, bundled files, configs, and packaging.
- `plugins/compiler`: assemblers and compilers.
- `plugins/cpu`: CPU implementations.
- `plugins/memory`: memory plugins.
- `plugins/device`: device plugins.

Related projects:
- `emuLib`: shared runtime and plugin API.
- `edigen`: instruction decoder/disassembler generator.
- `edigen-gradle-plugin`: Gradle integration for `edigen`.
- `cpu-testsuite`: CPU instruction test framework.
- `emustudio.github.io`: website and user/developer documentation.

## Before Editing
- Identify the affected Gradle module and entrypoint.
- Read nearby tests, configs, README/docs, and existing patterns.
- Check related projects when changing plugin APIs, generated CPU code, CPU tests, or public docs.
- Treat changes as large if they touch many modules, exceed about 200 lines, add dependencies, or change shared behavior.

## Editing Rules
- Keep behavior unchanged unless the task requires it.
- Follow `.editorconfig`; keep Java 11 compatibility.
- Preserve SPDX headers and GPL-3.0-or-later licensing. Warn and ask the user about further action if some code or files might violate this license. 
- Avoid unrelated cleanup, broad formatting, generated-file churn, and speculative abstractions.
- Do not hardcode paths, versions, or plugin contracts when Gradle config or existing constants own them.

## Java and Gradle
- Match existing package, module, and plugin structure.
- Keep application code in `application`; keep plugin code under the owning `plugins/...` module.
- Keep emulation timing, device I/O, CPU semantics, and binary formats explicit and tested.
- Update docs, configs, or examples when user-facing behavior or bundled computers change - modify user documentation in `emustudio.github.io` project.

## Validation
- Run the narrowest practical check first:
    - `./gradlew :<module>:test`
    - `./gradlew test`
    - `./gradlew build`
- For docs-only changes, run `git diff --check -- AGENTS.md`.
- If tests cannot run, say why and name the command that should be run.

## Final Response
- State what changed, files changed, tests run, docs/config impact, and related-project impact.

# Commits

- Commit messages must start with the Github issue
- based on changes, and known github open tickets, guess to what Github issue belongs the changes being commited. If not certain, guess it from the git branch name, which should be prefixed with `feature-XYZ` 
- when asked to amend, check the commit message and format and change it accordingly.
- Use this format:
    - first line: `[#XYZ] Add churn job`
    - blank line
    - bullet list of detailed changes, one short sentence each, max 80 chars
- Keep each commit scoped to one tightly related unit of change.
