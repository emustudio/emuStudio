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
