# ADR-001: Generic source editing metadata

## Status
Accepted

## Context
Issue #453 adds folding, include navigation, rename, and extraction to the source editor. The desktop editor must not
recognize particular assembler directives, symbol rules, or compiler implementations.

## Decision
emuLib owns generic block, symbol-definition, symbol-reference, and include-path token categories, optional symbol/scope
metadata, comment delimiters, and optional source-analysis/extraction operations on `LexicalAnalyzer`. Defaults preserve
existing lexer implementations. Offsets use Java String indices; editing snapshots contain immutable tokens.

Compiler plugins own language parsing, identifier validation, scope identification, and extraction safety. The 8080 and
Z80 assemblers share their source-analysis implementation through their existing compiler dependency. RAM and RASP
provide global label metadata. Brainc provides loop boundaries. No additional dependencies are introduced.

The application owns selections, search, previews, snapshot validation, navigation, and presentation. It resolves generic
symbol/scope and include metadata without language keywords. Folding combines block tokens, multiline comments, explicit
regions, and indentation. Unsupported semantic operations remain unavailable.

## Consequences
Old plugins continue to provide syntax highlighting and text editing. New semantic editing capabilities require rebuilt
plugins and emuLib. Rename rejects ambiguous declarations, repeated/cyclic includes, conflicts, and stale snapshots.
Extraction is offered only by supporting plugins and returns a reviewable source snapshot. These APIs introduce no
desktop dependencies into compiler plugins. No language server, AI, or completion integration is involved.
