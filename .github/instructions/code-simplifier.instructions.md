---
name: code-simplifier
description: simplifies and refines code for clarity, consistency, and maintainability while preserving exact functionality. use when chatgpt should clean up recently modified code, apply project-specific coding standards, reduce unnecessary complexity, improve readability, and keep behavior unchanged. especially useful after code edits, refactors, or generated code that works but is harder to read or inconsistent with project conventions.
---

Refine recently modified code to make it clearer, more consistent, and easier to maintain without changing functionality.

Focus on code that was edited or added in the current session unless the user explicitly asks for a broader review.

## Core rules

- Preserve exact behavior, outputs, side effects, and interfaces.
- Improve how the code is written, not what it does.
- Prefer readable, explicit code over compact or clever code.
- Do not optimize for fewer lines at the expense of clarity.
- Keep useful abstractions, but remove redundant ones.
- Avoid unnecessary comments that only restate obvious code.
- Document only meaningful changes that affect understanding.

## Apply project standards

Follow project conventions from `AGENTS.md` if present.

When simplifying code, prefer these standards:

- Use ES modules.
- Keep imports sorted and include required file extensions.
- Prefer the `function` keyword over arrow functions where appropriate.
- Add explicit return type annotations for top-level functions.
- Use explicit `Props` types for React components.
- Follow established naming conventions in the codebase.
- Prefer project-standard error handling patterns and avoid unnecessary `try/catch`.

If local code patterns conflict with generic style preferences, follow the established project pattern unless the user asks for standardization.

## Simplification guidelines

Look for opportunities to:

- Reduce unnecessary nesting.
- Replace awkward control flow with clearer structure.
- Eliminate redundant variables, wrappers, and abstractions.
- Consolidate closely related logic when it improves readability.
- Rename unclear variables or functions to better reflect intent.
- Make component and function structure more explicit and easier to scan.
- Keep separation of concerns intact.

For conditionals:

- Avoid nested ternary operators.
- Prefer `if/else` chains or `switch` statements for multi-branch logic.

## Avoid over-simplification

Do not simplify in ways that:

- Make the code harder to debug.
- Hide important intent.
- Merge too many responsibilities into one function or component.
- Introduce clever one-liners that reduce readability.
- Remove abstractions that help organization.
- Change public APIs, data flow, or expected behavior.

## Working process

1. Identify the code that was recently modified.
2. Inspect it for readability, consistency, and maintainability issues.
3. Apply relevant project conventions from `CLAUDE.md` and surrounding code.
4. Refine the implementation while preserving exact behavior.
5. Verify that the result is simpler, clearer, and still equivalent.
6. Mention only significant changes when a summary is useful.

## Output behavior

When making refinements:

- Edit the code directly when acting in an editing workflow.
- Keep explanations brief and focused on non-obvious improvements.
- If no meaningful simplification is needed, say so instead of forcing changes.

When summarizing changes, emphasize:

- structural simplifications
- consistency improvements
- naming or readability improvements
- any project-standard adjustments applied

