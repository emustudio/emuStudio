# emuStudio Agent Instructions

Talk like a caveman ultra.

AI agents should make changes in the correct repository and preserve the intended architecture of emuStudio.

## Architectural Direction

emuStudio should evolve as a **modular monolith** with clear internal boundaries.

Prefer:

* simple, explicit design;
* ports-and-adapters where core logic meets UI, filesystem, audio, or plugin loading;
* stable plugin APIs;
* small incremental refactorings;
* tests that protect behavior and module boundaries.

Avoid:

* broad rewrites;
* speculative abstractions;
* new frameworks without a clear reason;
* leaking desktop application internals into plugins or shared libraries.

## Sibling Repositories

- [emuLib](https://github.com/emustudio/emuLib): shared plugin API, runtime services, shared UI helpers, and reusable utilities.
- [edigen](https://github.com/emustudio/edigen): decoder/disassembler generator from `.eds` specifications.
- [emuStudio](https://github.com/emustudio/emuStudio): desktop application, bundled plugins, virtual computers, configs, and packaging.
- [emustudio.github.io](https://github.com/emustudio/emustudio.github.io): website, user documentation, developer documentation, and release-facing pages.
- [edigen-gradle-plugin](https://github.com/emustudio/edigen-gradle-plugin): Gradle task and DSL integration for Edigen source generation.
- [cpu-testsuite](https://github.com/emustudio/cpu-testsuite): shared CPU instruction test framework and reusable verification helpers.

When a task requires a sibling repository, first look for it as a local checkout, usually alongside this repository.

If it is not available locally, do not guess. Report that it is unavailable and continue with what can be done in the current repository.

## ADRs

Architecture Decision Records must be placed in `docs/adr`.

Create or update an ADR when changing:

* public plugin API;
* module boundaries;
* responsibilities between repositories;
* plugin loading;
* runtime services;
* project format;
* generated-code behavior;
* major dependencies.

Use a short ADR format:

```text
# ADR-NNN: Title

## Status
Proposed | Accepted | Superseded

## Context
Why is this needed?

## Decision
What are we changing?

## Consequences
What improves, what gets harder, and what must be migrated?
```

Do not create ADRs for trivial local implementation details.

## Tickets And Commits

Every change must have an existing GitHub ticket.

Every commit subject must start with:

```text
[#123] Short summary
```

If one task touches multiple repositories, use the same ticket prefix in each related commit.

Do not make unrelated changes under the same ticket.
