# Annotation metadata

In **Debugger**, use **Open annotations** to load annotation metadata for the
current computer. **Save annotations** writes an annotation-only copy.

An annotation export is a UTF-8 Java properties file. Each entry stores a CPU
address range, label, and code/data classification. The range end is exclusive.
Import skips entries with invalid ranges or classifications.

The computer's TOML file stores watches, breakpoints, and user annotations
under `[debugger]`. emuStudio restores them when it opens the computer.
