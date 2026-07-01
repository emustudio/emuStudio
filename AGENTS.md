# emuStudio Repo Routing

## Current Repository
- `emuStudio` owns the desktop application, CLI launcher, bundled official plugins, bundled virtual computers, configs, and distribution packaging.
- Main locations in this repository: `application`, `plugins/compiler`, `plugins/cpu`, `plugins/memory`, `plugins/device`, and `application/src/main/files/config`.

## Sibling Repositories
- [emuLib](https://github.com/emustudio/emuLib): shared plugin API, runtime services, shared UI helpers, and reusable utilities.
- [edigen](https://github.com/emustudio/edigen): decoder/disassembler generator from `.eds` specifications.
- [emuStudio](https://github.com/emustudio/emuStudio): desktop application, bundled plugins, virtual computers, configs, and packaging.
- [emustudio.github.io](https://github.com/emustudio/emustudio.github.io): website, user documentation, developer documentation, and release-facing pages.
- [edigen-gradle-plugin](https://github.com/emustudio/edigen-gradle-plugin): Gradle task and DSL integration for Edigen source generation.
- [cpu-testsuite](https://github.com/emustudio/cpu-testsuite): shared CPU instruction test framework and reusable verification helpers.

When a task calls for checking or updating a sibling repository, first look for it as a local checkout (typically alongside this repository). If it is present locally, work with it there. If it is not present locally, do not guess its location or assume changes were made; report that the repository is not available locally and continue with what can be done in this repository.

## When To Update Which Repository
- Desktop app behavior, CLI behavior, plugin wiring, bundled virtual computers, bundled configs, or packaging: update `emuStudio`.
- Shared plugin API or runtime behavior used across plugins: update `emuLib`; then check `emuStudio`, `edigen`, and `cpu-testsuite`.
- Generated decoder or disassembler behavior for CPU plugins: update `edigen`; also check affected CPU plugins in `emuStudio` and Gradle integration in `edigen-gradle-plugin`.
- Shared CPU instruction testing support: update `cpu-testsuite`; check CPU plugin tests in `emuStudio`.
- User or developer documentation, website pages, or download/release pages: update `emustudio.github.io`.

## Tickets And Commits
- Every change must have an existing GitHub ticket.
- Every commit subject must start with the ticket prefix: `[#123] Short summary`.
- If one task touches multiple emuStudio repositories, use the same ticket prefix in each related commit.
