# emuStudio documentation

This module contains all the official documentation for emuStudio. Besides user manual it contains also a set of
tutorials for plug-in development.

All the documentation is written in Asciidoc format. 

## How to "compile" the documentation?

Before installation, please install Graphviz on your computer. The `dot` program must be in path.
Graphviz can be downloaded from http://www.graphviz.org/Download.php

Then, run the following command:

```
mvn clean install -P doc
```

## Advices for documenting a feature

Documentation should be clear and balanced in both form and content. In order to achieve that, the following
questions should be helpful:

 - What is the purpose of this feature? How it will be used and by whom?
 - What changes happened in UI?
 - How this feature works inside, simply explained?
 - How is this feature configured? What changes happened in configuration files?
 - How configuration options relate to each other, what are sensible defaults and what's the motivation for changing them?
 - What kind of general troubles can we expect at the customer, how do we diagnose them and how to solve them?
 - How a typical deployment looks like? Are there any tips for deployment and sizing?
 - Does this feature has any performance impact? Under which conditions and how to troubleshoot possible problems?
 - Any special advices or notes for upgrading from latest versions?
