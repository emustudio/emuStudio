# Annotation metadata

In **Inspection**, use **Import annotations** to load annotation metadata for the
current computer. **Export annotations** writes an annotation-only copy.

The UTF-8 Java properties file contains format version `4`, `image.identity`, and
the entry count. Each entry stores its identity, CPU address range, label,
and code/data classification. The range end is exclusive.

Import requires a matching image identity and valid address ranges. The format
is also used in the computer's `computer.toml.debugger` file, which stores
annotations, watches and breakpoint rules together. Memory contents and execution
recordings use separate files.
