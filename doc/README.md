# emuStudio user documentation

In here, you can see user documentation fot emuStudio main module. Documentation of plug-ins
is there where the plug-ins are (or at least should be).

## How to "compile" the documentation?

The files you can see are written in Asciidoc format. So in order to have it compiled to
HTML or PDF (or whatever format asciidoc supports), you must install asciidoc first.

For most Linux distributions, it's available in default repositories for the distribution.
For windows (and for more information), go to
[this](http://www.methods.co.nz/asciidoc/INSTALL.html#X3) page.

Then, `graphviz` is needed as well. For Windows, see
[this](http://www.graphviz.org/Download_windows.php) page.

After this, HTML can be compiled with command:

    asciidoc index.txt

The program will output everything (HTML files, CSS, images) and the documentation can be used.

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
