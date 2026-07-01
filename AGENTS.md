# emuStudio Repo Routing

## Current Repository
- `emuStudio` owns the desktop application, CLI launcher, bundled official plugins, bundled virtual computers, configs, and distribution packaging.
- Main locations in this repository: `application`, `plugins/compiler`, `plugins/cpu`, `plugins/memory`, `plugins/device`, and `application/src/main/files/config`.

## Sibling Repositories
- `/home/vbmacher/projects/emustudio/emuLib`: shared plugin API, runtime services, shared UI helpers, and reusable utilities.
- `/home/vbmacher/projects/emustudio/edigen`: decoder/disassembler generator from `.eds` specifications.
- `/home/vbmacher/projects/emustudio/emuStudio`: desktop application, bundled plugins, virtual computers, configs, and packaging.
- `/home/vbmacher/projects/emustudio/emustudio.github.io`: website, user documentation, developer documentation, and release-facing pages.
- `/home/vbmacher/projects/emustudio/edigen-gradle-plugin`: Gradle task and DSL integration for Edigen source generation.
- `/home/vbmacher/projects/emustudio/cpu-testsuite`: shared CPU instruction test framework and reusable verification helpers.

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
