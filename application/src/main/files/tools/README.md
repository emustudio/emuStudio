# Annotation metadata

In the Debugger tab, open **Annotations**, then **Manage… → Import annotations…**
to load annotation metadata for the current memory image.

The UTF-8 Java properties file contains format version `3`, `image.identity`, and
the entry count. Each entry stores its identity, CPU address range, label,
comment, and code/data classification. The range end is exclusive.

Import requires a matching image identity and valid address ranges. The format
stores annotations only; memory contents and execution recordings use separate
files.
